package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
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

case class DeleteVideoInfoMessagePlanner(
                                          token: String,
                                          videoID: Int,
                                          override val planContext: PlanContext
                                        ) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Validate user token
      _ <- IO(logger.info(s"Validating token: ${token}"))
      userIDOption <- getUIDByToken

      response <- userIDOption match {
        case None =>
          IO(logger.warn("Invalid token, no user ID found")) *> IO.pure(Some("Invalid Token"))
        case Some(userID) =>
          for {
            // Step 2: Verify if the user is the uploader of the video
            _ <- IO(logger.info(s"Checking if user ${userID} is the uploader of video ID: ${videoID}"))
            isUploader <- checkUploader(userID)

            result <- if (!isUploader) {
              IO(logger.warn(s"Permission denied for user ${userID} to delete video ${videoID}")) *>
                IO.pure(Some("Permission Denied"))
            } else {
              // Step 3: Delete video metadata
              IO(logger.info(s"Deleting video metadata for video ID: ${videoID}"))
              val deleteVideoResult =

              // Step 4: Return result
              IO(logger.info("Returning result after deletion operation"))
              deleteVideoMetadata.flatMap {
                case true => IO.pure(None)
                case false =>
                  IO(logger.warn("Failed to delete video information")) >>
                  IO.pure(Some("Unable to delete video information"))
              }
            }
          } yield result
      }
    } yield response
  }

  private def getUIDByToken(using PlanContext): IO[Option[Int]] = {
    logger.info("Calling API to validate token and get user ID")
    GetUIDByTokenMessage(token).send
  }

  private def checkUploader(userID: Int)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         |SELECT uploader_id
         |FROM ${schemaName}.video_info_table
         |WHERE video_id = ?;
       """.stripMargin

    logger.info(s"Executing SQL to validate uploader: ${sql}")
    readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
      case None =>
        logger.warn(s"Video ID ${videoID} not found in VideoInfoTable")
        false
      case Some(json) =>
        val uploaderID = decodeField[Int](json, "uploader_id")
        uploaderID == userID
    }
  }

  private def deleteVideoMetadata(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         |DELETE FROM ${schemaName}.video_info_table
         |WHERE video_id = ?;
       """.stripMargin

    logger.info(s"Executing SQL to delete video metadata: ${sql}")
    writeDB(sql, List(SqlParameter("Int", videoID.toString))).attempt.map {
      case Right(_) =>
        logger.info(s"Successfully deleted video metadata for video ID: ${videoID}")
        true
      case Left(e) =>
        logger.error(s"Error while deleting video metadata for video ID ${videoID}: ${e.getMessage}")
        false
    }
  }
}