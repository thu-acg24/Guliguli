package Utils

import APIs.UserService.{GetUIDByTokenMessage, QueryUserRoleMessage}
import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.minioClient
import Objects.UserService.UserRole
import Objects.VideoService.{Video, VideoInfo, VideoStatus}
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.GetPresignedObjectUrlArgs
import io.minio.http.Method
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

//process plan import 预留标志位，不要删除

case object VideoAuth {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除

  // implicit val dateTimeDecoder: Decoder[DateTime] = decodeDateTime
  def validateToken(token: Option[String])(using PlanContext): IO[(Option[Int], Option[UserRole])] = {
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

  def validateVideoID(videoID: Int, user: (Option[Int], Option[UserRole]))
                             (using PlanContext): IO[Unit] = {
    val (userID, role) = user
    for {
      _ <- IO(logger.info(s"[Step 2] Validating videoID: $videoID"))
      videoQueryResult <- readDBJsonOptional(
        s"""
          SELECT uploader_id, status
          FROM ${schemaName}.video_table
          WHERE video_id = ?;
        """.stripMargin,
        List(SqlParameter("Int", videoID.toString))
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
}