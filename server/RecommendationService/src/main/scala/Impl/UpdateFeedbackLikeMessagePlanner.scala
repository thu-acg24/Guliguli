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

case class UpdateFeedbackLikeMessagePlanner(
                                             token: String,
                                             videoID: Int,
                                             isLike: Boolean,
                                             override val planContext: PlanContext
                                           ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate Token and get User ID
      _ <- IO(logger.info(s"Validating token: '$token'"))
      userID <- GetUIDByTokenMessage(token).send

      _ <- IO(logger.info(s"User ID: $userID"))

      // Step 2: Check if videoID exists
      _ <- IO(logger.info(s"Checking video existence for video ID: $videoID"))
      video <- QueryVideoInfoMessage(None, videoID).send

      _ <- IO(logger.info(s"Video valid: ID=${video.videoID}, Title=${video.title}"))

      // Step 3: Perform add/remove favorite based on isFavorite
      result <- handleLikeAction(userID, videoID)
    } yield result
  }

  private def handleLikeAction(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    if (isLike) performLikeAction(userID, videoID)
    else performUnlikeAction(userID, videoID)
  }

  private def performLikeAction(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    return IO.unit
  }

  private def performUnlikeAction(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    return IO.unit
  }
}