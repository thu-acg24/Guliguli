package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.APIException.InvalidInputException
import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import Utils.DecodeVideo.decodeVideo
import Utils.VideoAuth.{validateToken, validateVideoID}
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.slf4j.LoggerFactory

case class QueryVideoInfoMessagePlanner(
                                         token: Option[String],
                                         videoID: Int,
                                         override val planContext: PlanContext
                                       ) extends Planner[Video] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Video] = {
    for {
      // Step 1: Validate token to ensure user access authorization
      user <- validateToken(token)
      // Step 2: Validate if the video ID is valid and accessible
      _ <- validateVideoID(videoID, user)
      // Step 3: Query video information if validation passes
      video <- queryVideo()
      _ <- IO(logger.info(s"[Plan Completed] Video query result: $video"))
    } yield video
  }

  private def queryVideo()(using PlanContext): IO[Video] = {
    for {
      _ <- IO(logger.info(s"[Step 3] Querying detailed information for videoID: $videoID"))
      videoQueryResult <- readDBJsonOptional(
        s"""
          SELECT video_id, title, description, duration, tag, cover,
                 uploader_id, views, likes, favorites, status, upload_time
          FROM ${schemaName}.video_table
          WHERE video_id = ?;
        """.stripMargin,
        List(SqlParameter("Int", videoID.toString))
      )

      video <- videoQueryResult match {
        case Some(json) =>
          IO(logger.info(s"[Step 3.1] Video information found, json: $json")) *>
            decodeVideo(json)
        case None =>
          IO(logger.info("[Step 3.1] No video details found in the database")) >>
          IO.raiseError(InvalidInputException("Video does not exist"))
      }
    } yield video
  }
}