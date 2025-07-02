package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.APIException.InvalidInputException
import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryVideoInfoMessagePlanner(
                                         token: Option[String],
                                         videoId: Int,
                                         override val planContext: PlanContext
                                       ) extends Planner[Video] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Video] = {
    for {
      // Step 1: Validate token to ensure user access authorization
      userInfo <- validateToken()
      // Step 2: Validate if the video ID is valid and accessible
      _ <- validateVideoID(userInfo)
      // Step 3: Query video information if validation passes
      videoInfo <- queryVideoInfo()
      _ <- IO(logger.info(s"[Plan Completed] Video query result: $videoInfo"))
    } yield videoInfo
  }

  private def validateToken()(using PlanContext): IO[(Option[Int], Option[UserRole])] = {
    token match {
      case Some(tkn) =>
        for {
          _ <- IO(logger.info(s"[Step 1.1] Validating token: $tkn"))
          userID <- GetUIDByTokenMessage(tkn).send
          _ <- IO(logger.info(s"[Step 1.2] Fetched userID: $userID"))

          role <- QueryUserRoleMessage(tkn).send
          _ <- IO(logger.info(s"[Step 1.3] Fetched userRole: $role"))
        } yield (Some(userID), Some(role))
      case None =>
        IO(logger.info("[Step 1.1] No token provided, skipping authentication")) *> IO.pure((None, None))
    }
  }

  private def validateVideoID(userInfo: (Option[Int], Option[UserRole]))(using PlanContext): IO[Unit] = {
    val (userID, role) = userInfo
    for {
      _ <- IO(logger.info(s"[Step 2] Validating videoID: $videoId"))
      videoQueryResult <- readDBJsonOptional(
        s"""
          SELECT uploader_id, status
          FROM ${schemaName}.video_table
          WHERE video_id = ?;
        """.stripMargin,
        List(SqlParameter("Int", videoId.toString))
      )

      _ <- videoQueryResult match {
        case Some(json) =>
          val uploaderID = decodeField[Int](json, "uploader_id")
          val status = VideoStatus.fromString(decodeField[String](json, "status"))

          if (status == VideoStatus.Approved || (userID.contains(uploaderID) || role.contains(UserRole.Auditor))) {
            IO(logger.info("[Step 2.1] Video validation passed")) *> IO.unit
          } else {
            IO(logger.info("[Step 2.1] Video is not public or user has no access permission")) >>
              IO.raiseError(InvalidInputException("Video does not exist"))
          }
        case None =>
          IO(logger.info("[Step 2.1] Video does not exist")) >>
            IO.raiseError(InvalidInputException("Video does not exist"))
      }
    } yield ()
  }

  private def queryVideoInfo()(using PlanContext): IO[Video] = {
    for {
      _ <- IO(logger.info(s"[Step 3] Querying detailed information for videoID: $videoId"))
      videoQueryResult <- readDBJsonOptional(
        s"""
          SELECT video_id, title, description, duration, tag, server_path, cover_path,
                 uploader_id, views, likes, favorites, status, upload_time
          FROM ${schemaName}.video_table
          WHERE video_id = ?;
        """.stripMargin,
        List(SqlParameter("Int", videoId.toString))
      )

      video <- videoQueryResult match {
        case Some(json) =>
          IO(logger.info("[Step 3.1] Video information found, decoding into Video object")) *>
            IO(decodeType[Video](json))
        case None =>
          IO(logger.info("[Step 3.1] No video details found in the database")) >>
          IO.raiseError(InvalidInputException("Video does not exist"))
      }
    } yield video
  }
}