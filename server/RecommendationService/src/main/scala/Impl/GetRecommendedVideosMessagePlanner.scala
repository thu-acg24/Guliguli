package Impl


import Common.APIException.InvalidInputException
import APIs.UserService.{GetUIDByTokenMessage, QueryUserInfoMessage}
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.ServiceUtils.schemaName
import Objects.PGVector
import Objects.PGVector.defaultDim
import Objects.VideoService.Video
import Utils.PerferenceProcess.{fetchVideoDetails, getUserVector, getVideoVector}
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.slf4j.LoggerFactory

import java.util.UUID

case class GetRecommendedVideosMessagePlanner(
    videoID: Option[Int],
    userToken: Option[String],
    randomRatio: Float,
    fetchLimit: Int = 20,
    override val planContext: PlanContext
) extends Planner[List[Video]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[Video]] = {
    for {
      // Step 1: Validate input parameters
      _ <- IO(logger.info(s"确认userToken合法性"))
      userID <- userToken.traverse(GetUIDByTokenMessage(_).send)
      _ <- IO(logger.info(s"校验入参，确保随机的权重必须要在0.1-0.9之间"))
      _ <- validateInputParams(userID, videoID)

      // Step 2: Generate recommended video IDs
      _ <- IO(logger.info(s"根据用户行为或视频标签生成推荐视频ID列表"))
      recommendedVideoIDs <- generateRecommendedVideoIDs(userID, videoID)

      // Step 3: Fetch full video details
      _ <- IO(logger.info(s"将推荐视频ID列表映射为完整视频对象"))
      recommendedVideos <- fetchVideoDetails(recommendedVideoIDs)
    } yield recommendedVideos
  }

  private def validateInputParams(userID: Option[Int], videoID: Option[Int])(using PlanContext): IO[Unit] = {
    if (randomRatio < 0.1 || randomRatio > 0.9)
      IO.raiseError(InvalidInputException("随机的权重必须要在0.1-0.9之间"))
    else
      for {
        _ <- videoID match {
          case Some(id) =>
            IO(logger.info(s"验证视频ID $id 是否有效"))
              .flatMap(_ => QueryVideoInfoMessage(None, id).send.void)
          case None => IO.unit
        }
        _ <- userID match {
          case Some(id) =>
            IO(logger.info(s"验证用户ID $id 是否有效"))
              .flatMap(_ => QueryUserInfoMessage(id).send.void)
          case None => IO.unit
        }
      } yield ()
  }

  private def generateRecommendedVideoIDs(userID: Option[Int], videoID: Option[Int])(using PlanContext): IO[List[Int]] = {
    val sql =
      s"""
         |WITH nearest_candidates AS (
         |  SELECT video_id, view_count, embedding <#> ? AS neg_dot_product
         |  FROM $schemaName.video_info_table
         |  WHERE visible = true
         |  ${videoID.fold("")(_ => "AND video_id != ?")}
         |  ORDER BY neg_dot_product ASC
         |  LIMIT 200
         |)
         |SELECT video_id,
         |       neg_dot_product - (0.05 * log(10, GREATEST(view_count, 1))) AS neg_combined_score
         |FROM nearest_candidates
         |ORDER BY neg_combined_score ASC
         |LIMIT $fetchLimit;
         |""".stripMargin
    for {
      videoVector <- videoID match {
        case Some(id) => getVideoVector(id)
        case None => IO.pure(PGVector(Vector.fill(defaultDim)(0.0F)))
      }
      userVector <- userID match {
        case Some(id) => getUserVector(id)
        case None => IO.pure(PGVector(Vector.fill(defaultDim)(0.0F)))
      }
      randomVector <- PGVector.fromString(UUID.randomUUID.toString)
      queryVector <- IO.pure((
        randomVector * randomRatio +
        videoVector * ((1 - randomRatio) * userID.fold(1.0F)(_ => 0.625F)) +
        userVector * ((1 - randomRatio) * videoID.fold(1.0F)(_ => 0.375F))
      ).normalize)
      resultIDs <-
        readDBRows(sql,
          SqlParameter("Vector", queryVector.toString) +:
          videoID.fold(List())
            (id => List(SqlParameter("Int", id.toString)))
        ).map(_.map(json => decodeField[Int](json, "video_id")))
    } yield resultIDs
  }
}