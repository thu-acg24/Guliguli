package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.PGVector
import Objects.PGVector.defaultDim
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import Utils.PerferenceProcess.{fetchVideoDetails, getUserVector}
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import java.util.UUID

case class SearchVideosMessagePlanner(
    token: Option[String],
    searchString: String,
    fetchLimit: Int = 20,
    override val planContext: PlanContext
) extends Planner[List[Video]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[Video]] = {
    logger.info(s"执行SearchVideosMessage，searchString=$searchString")
    for {
      // Step 1: Validate input parameters
      _ <- IO(logger.info(s"确认userToken合法性"))
      userID <- token.traverse(GetUIDByTokenMessage(_).send)

      // Step 2: 查询
      videoIDs <- getMatchingVideos(userID, searchString)


      // Step 3: 转回Video对象
      _ <- IO(logger.info(s"将推荐视频ID列表映射为完整视频对象"))
      videos <- fetchVideoDetails(videoIDs)
    } yield videos
  }

  private def getMatchingVideos(
                                         userID: Option[Int],
                                         keyword: String
                                       )(using PlanContext): IO[List[Int]] = {

    val words = keyword.split("\\s+").filter(_.nonEmpty)
    val likeConditions = words.map(_ => "description ILIKE ?").toList
    val whereFilterClause = if likeConditions.isEmpty then "TRUE" else likeConditions.mkString(" AND ")

    val sql =
      s"""
         |WITH filtered_candidates AS (
         |  SELECT video_id, view_count, embedding <#> ? AS neg_dot_product
         |  FROM $schemaName.video_info_table
         |  WHERE visible = true AND $whereFilterClause
         |  ORDER BY neg_dot_product ASC
         |  LIMIT 200
         |)
         |SELECT video_id,
         |       neg_dot_product - (0.05 * log(10, GREATEST(view_count, 1))) AS neg_combined_score
         |FROM filtered_candidates
         |ORDER BY neg_combined_score ASC
         |LIMIT $fetchLimit;
         |""".stripMargin

    for {
      userVector <- userID match {
        case Some(id) => getUserVector(id)
        case None     => IO.pure(PGVector(Vector.fill(defaultDim)(0.0f)))
      }

      randomVector <- PGVector.fromString(UUID.randomUUID.toString)

      queryVector = (
        randomVector * 0.3f +
          userVector * (1.0f - 0.3f)
        ).normalize
      _ <- IO(logger.info("Started searching videos..."))
      likeParams = words.map(w => SqlParameter("String", s"%$w%")).toList
      allParams = SqlParameter("Vector", queryVector.toString) +: likeParams

      resultIDs <- readDBRows(sql, allParams).map(_.map(json => decodeField[Int](json, "video_id")))
    } yield resultIDs
  }
}