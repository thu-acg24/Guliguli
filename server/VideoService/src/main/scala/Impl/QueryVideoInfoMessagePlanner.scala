package Impl


import Objects.VideoService.VideoStatus
import Objects.VideoService.Video
import Objects.UserService.UserRole
import APIs.UserService.QueryUserRoleMessage
import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName
import APIs.UserService.getUIDByTokenMessage
import Objects.VideoService.{Video, VideoStatus}
import APIs.UserService.{QueryUserRoleMessage, getUIDByTokenMessage}
import io.circe.Json
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryVideoInfoMessagePlanner(
                                         token: Option[String],
                                         videoId: Int,
                                         override val planContext: PlanContext
                                       ) extends Planner[Option[Video]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[Video]] = {
    for {
      // Step 1: Validate token to ensure user access authorization
      userInfo <- validateToken()
      // Step 2: Validate if the video ID is valid and accessible
      videoValidation <- validateVideoID(userInfo)
      // Step 3: Query video information if validation passes
      videoInfo <- if (videoValidation) queryVideoInfo() else IO.pure(None)
      _ <- IO(logger.info(s"[Plan Completed] Video query result: $videoInfo"))
    } yield videoInfo
  }

  private def validateToken()(using PlanContext): IO[(Option[Int], Option[UserRole])] = {
    token match {
      case Some(tkn) =>
        for {
          _ <- IO(logger.info(s"[Step 1.1] Validating token: $tkn"))
          userID <- getUIDByTokenMessage(tkn).send
          _ <- IO(logger.info(s"[Step 1.2] Fetched userID: $userID"))

          role <- userID match {
            case Some(_) =>
              for {
                userRole <- QueryUserRoleMessage(tkn).send
                _ <- IO(logger.info(s"[Step 1.3] Fetched userRole: $userRole"))
              } yield userRole
            case None =>
              IO(logger.info("[Step 1.3] Invalid token, unable to fetch user role")) *> IO.pure(None)
          }
        } yield (userID, role)
      case None =>
        IO(logger.info("[Step 1.1] No token provided, skipping authentication")) *> IO.pure((None, None))
    }
  }

  private def validateVideoID(userInfo: (Option[Int], Option[UserRole]))(using PlanContext): IO[Boolean] = {
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

      isValid <- videoQueryResult match {
        case Some(json) =>
          val uploaderID = decodeField[Int](json, "uploader_id")
          val status = VideoStatus.fromString(decodeField[String](json, "status"))

          if (status == VideoStatus.Approved || (userID.contains(uploaderID) || role.contains(UserRole.Admin))) {
            IO(logger.info("[Step 2.1] Video validation passed")) *> IO.pure(true)
          } else {
            IO(logger.info("[Step 2.1] Video is not public or user has no access permission")) *> IO.pure(false)
          }
        case None =>
          IO(logger.info("[Step 2.1] Video does not exist")) *> IO.pure(false)
      }
    } yield isValid
  }

  private def queryVideoInfo()(using PlanContext): IO[Option[Video]] = {
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
            IO.pure(Some(decodeType[Video](json)))
        case None =>
          IO(logger.info("[Step 3.1] No video details found in the database")) *> IO.pure(None)
      }
    } yield video
  }
}