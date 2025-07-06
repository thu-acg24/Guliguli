package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.APIException.InvalidInputException
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
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
      video <- QueryVideoInfoMessage(Some(token), videoID).send
      _ <- IO.raiseUnless(video.uploaderID == userID)(InvalidInputException("用户不是视频上传者"))
      // Step 3: Delete video metadata
      _ <- IO(logger.info(s"Deleting video metadata for video ID: ${videoID}"))
      - <- deleteVideoMetadata
    } yield ()
  }

  private def getUIDByToken(using PlanContext): IO[Int] = {
    GetUIDByTokenMessage(token).send
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