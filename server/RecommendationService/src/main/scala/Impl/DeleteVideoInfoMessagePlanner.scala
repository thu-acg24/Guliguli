package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.APIException.InvalidInputException
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class DeleteVideoInfoMessagePlanner(
                                          token: String,
                                          videoID: Int,
                                          override val planContext: PlanContext
                                        ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate user token
      _ <- IO(logger.info(s"Validating token: ${token}"))
      userID <- getUIDByToken
      // Step 2: Verify if the user is the uploader of the video
      _ <- IO(logger.info(s"Checking if user ${userID} is the uploader of video ID: ${videoID}"))
      _ <- checkUploader(userID)
      // Step 3: Delete video metadata
      _ <- IO(logger.info(s"Deleting video metadata for video ID: ${videoID}"))
      - <- deleteVideoMetadata
    } yield ()
  }

  private def getUIDByToken(using PlanContext): IO[Int] = {
    GetUIDByTokenMessage(token).send
  }

  private def checkUploader(userID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
         |SELECT uploader_id
         |FROM ${schemaName}.video_info_table
         |WHERE video_id = ?;
       """.stripMargin

    IO(logger.info(s"Executing SQL to validate uploader: ${sql}"))>>
    readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
      case None =>
        IO.raiseError(InvalidInputException("Video Not Found"))
      case Some(json) =>
        val uploaderID = decodeField[Int](json, "uploader_id")
        if(uploaderID != userID)
          IO.raiseError(InvalidInputException("You're Not the Uploader Of Video"))
        else IO.unit
    }
  }

  private def deleteVideoMetadata(using PlanContext): IO[Unit] = {
    val sql =
      s"""
         |DELETE FROM ${schemaName}.video_info_table
         |WHERE video_id = ?;
       """.stripMargin

    IO(logger.info(s"Executing SQL to delete video metadata: ${sql}"))>>
    writeDB(sql, List(SqlParameter("Int", videoID.toString))).as(())
  }
}