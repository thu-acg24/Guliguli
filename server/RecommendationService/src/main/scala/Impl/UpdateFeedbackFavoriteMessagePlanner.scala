package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import Utils.PerferenceProcess.updateEmbedding
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * UpdateFeedbackFavoriteMessagePlanner: 根据用户的收藏或取消收藏行为，更新相应记录。
 *
 * @param token 用户的访问令牌
 * @param videoID 视频唯一标识
 * @param isFavorite 是否标记为收藏
 */

case class UpdateFeedbackFavoriteMessagePlanner(
  token: String,
  videoID: Int,
  isFavorite: Boolean,
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
      result <- handleFavoriteAction(userID, videoID)
    } yield result
  }

  private def handleFavoriteAction(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    if (isFavorite) updateEmbedding(userID, videoID, None, Some(0.15F))
    else updateEmbedding(userID, videoID, None, Some(-0.12F))
  }
}