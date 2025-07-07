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
import Utils.PerferenceProcess.{getUserVector, getVideoVector}
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
    override val planContext: PlanContext
) extends Planner[List[Video]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[Video]] = {
    for {
      // Step 1: Validate input parameters
      _ <- IO(logger.info(s"确认userToken合法性"))
      userID <- userToken.traverse(GetUIDByTokenMessage(_).send)
      _ <- IO(logger.info(s"校验入参，确保 videoID 或 userID 至少有一个有效值"))
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
    if (videoID.isEmpty && userID.isEmpty)
      IO.raiseError(InvalidInputException("videoID 和 userID 至少需要一个有效值"))
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
         |  SELECT video_id, view_count, embedding <#> ? AS dot_product
         |  FROM $schemaName.video_info_table
         |  ORDER BY embedding <#> ? DESC
         |  LIMIT 200
         |)
         |SELECT video_id,
         |       dot_product + (0.2 * log(10, GREATEST(view_count, 1))) AS combined_score
         |FROM nearest_candidates
         |ORDER BY combined_score DESC
         |LIMIT 20;
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
          List(SqlParameter("Vector", queryVector.toString)).flatMap(x => List(x, x))
        ).map(_.map(json => decodeField[Int](json, "video_id")))
    } yield resultIDs
  }

  private def generateRecommendationsFromTags(tags: List[String]): IO[List[Int]] = {
    val dummyRecommendations = tags.flatMap(tag => List(tag.hashCode.abs % 100)) // 简化示例
    IO(logger.info(s"基于标签生成的推荐视频ID：$dummyRecommendations")).map(_ => dummyRecommendations)
  }

  private def fetchVideoDetails(videoIDs: List[Int])(using PlanContext): IO[List[Video]] = {
    videoIDs match {
      case Nil => IO.pure(List.empty)
      case ids =>
        IO(logger.info(s"开始根据ID获取视频完整信息，共 ${ids.length} 个")) *>
          ids.traverse(fetchSingleVideo)
    }
  }

  private def fetchSingleVideo(videoID: Int)(using PlanContext): IO[Video] = {
    QueryVideoInfoMessage(None, videoID).send
  }
}