package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.RecommendationService.VideoInfo
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import Utils.PerferenceProcess.getInfo
import cats.effect.IO
import cats.implicits.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class UpdateVideoInfoMessagePlanner(
    token: String,
    videoID: Int,
    override val planContext: PlanContext
) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      // Step 1. Validate the token and retrieve the user ID
      _ <- IO(logger.info(s"Starting token validation for token=${token}"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2. Fetch video
      _ <- IO(logger.info(s"Starting ownership validation for videoID=${videoID}"))
      video <- QueryVideoInfoMessage(Some(token), videoID).send

      // Step 3. Update the video metadata in VideoInfoTable according to the video
      _ <- updateVideoMetadata(video)
    } yield ()
  }


  // Sub-step 3.1: Update video information in the database
  private def updateVideoMetadata(video: Video)(using PlanContext): IO[String] = {
    val sql =
      s"""
      UPDATE ${schemaName}.video_info_table
      SET title = ?, visible = ?, embedding = ?::vector
      WHERE video_id = ?;
      """.stripMargin

    val parameters = List(
      SqlParameter("String", video.title + video.description),
      SqlParameter("Boolean", (video.status == VideoStatus.Approved).toString),
      SqlParameter("Vector", getInfo(video.tag).toString),
      SqlParameter("String", videoID.toString),
    )

    writeDB(sql, parameters)
  }
}