package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.RecommendationService.VideoInfo
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class UpdateVideoInfoMessagePlanner(
    token: String,
    info: VideoInfo,
    override val planContext: PlanContext
) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      // Step 1. Validate the token and retrieve the user ID
      _ <- IO(logger.info(s"Starting token validation for token=${token}"))
      maybeUserID <- getUserIDFromToken()

      // Step 2. Check video ownership for the user
      _ <- IO(logger.info(s"Starting ownership validation for videoID=${info.videoID}"))
      isUploader <- maybeUserID match {
        case Some(userID) =>
          validateUploader(userID, info.videoID)
        case None =>
          IO.pure(Some("Invalid Token"))
      }

      // Step 3. Update the video metadata in VideoInfoTable if valid
      updateResult <- if (isUploader.isEmpty) {
        IO(logger.info(s"Updating video metadata for videoID=${info.videoID}")) >>
          updateVideoMetadata(info)
      } else IO.pure(isUploader)

      // Step 4. Log the final result
      _ <- IO(logger.info(s"Operation result for videoID=${info.videoID}: ${updateResult.getOrElse("Success")}"))
    } yield updateResult
  }

  // Sub-step 1.1: Retrieve User ID based on the token
  private def getUserIDFromToken()(using PlanContext): IO[Option[Int]] = {
    GetUIDByTokenMessage(token).send
  }

  // Sub-step 2.1: Validate if the user is the uploader of the video
  private def validateUploader(userID: Int, videoID: Int)(using PlanContext): IO[Option[String]] = {
    QueryVideoInfoMessage(Some(token), videoID).send.flatMap {
      case Some(video) if video.uploaderID == userID =>
        IO(None) // User is the uploader
      case Some(_) =>
        IO(Some("Unauthorized Operation")) // User is not the uploader
      case None =>
        IO(Some("Video Not Found")) // Video not found
    }
  }

  // Sub-step 3.1: Update video information in the database
  private def updateVideoMetadata(info: VideoInfo)(using PlanContext): IO[Option[String]] = {
    val sql =
      s"""
      UPDATE ${schemaName}.video_info_table
      SET title = ?, description = ?, tag = ?, views = ?, likes = ?, favorites = ?, visible = ?
      WHERE video_id = ?;
      """.stripMargin

    val parameters = List(
      SqlParameter("String", info.title),
      SqlParameter("String", info.description),
      SqlParameter("Array[String]", info.tag.asJson.noSpaces),
      SqlParameter("Int", info.views.toString),
      SqlParameter("Int", info.likes.toString),
      SqlParameter("Int", info.favorites.toString),
      SqlParameter("Boolean", info.visible.toString),
      SqlParameter("Int", info.videoID.toString)
    )

    writeDB(sql, parameters).map(_ => None).handleErrorWith { err =>
      IO(logger.error(s"Database update failed: ${err.getMessage}")) >>
        IO(Some("Unable to update video information"))
    }
  }
}