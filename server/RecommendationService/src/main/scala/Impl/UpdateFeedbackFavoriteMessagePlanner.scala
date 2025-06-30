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
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * UpdateFeedbackFavoriteMessagePlanner: 根据用户的收藏或取消收藏行为，更新相应记录。
 *
 * @param token 用户的访问令牌
 * @param videoID 视频唯一标识
 * @param isFavorite 是否标记为收藏
 * @param planContext 请求执行的上下文
 */

case class UpdateFeedbackFavoriteMessagePlanner(
  token: String,
  videoID: Int,
  isFavorite: Boolean,
  override val planContext: PlanContext
) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Validate Token and get User ID
      _ <- IO(logger.info(s"Validating token: '$token'"))
      maybeUserID <- GetUIDByTokenMessage(token).send
      userID <- maybeUserID match {
        case Some(id) => IO.pure(id)
        case None =>
          IO(logger.error("Invalid Token")) >>
            IO.pure(Some("Invalid Token"))
      }

      _ <- IO(logger.info(s"User ID: $userID"))

      // Step 2: Check if videoID exists
      _ <- IO(logger.info(s"Checking video existence for video ID: $videoID"))
      maybeVideo <- QueryVideoInfoMessage(None, videoID).send
      video <- maybeVideo match {
        case Some(video) if video.status == VideoStatus.Approved => IO.pure(video)
        case _ =>
          IO(logger.error(s"Video Not Found for ID: '$videoID' or not approved.")) >>
            IO.pure(Some("Video Not Found"))
      }

      _ <- IO(logger.info(s"Video valid: ID=${video.videoID}, Title=${video.title}"))

      // Step 3: Perform add/remove favorite based on isFavorite
      result <- handleFavoriteAction(userID, videoID)
    } yield result
  }

  private def handleFavoriteAction(userID: Int, videoID: Int)(using PlanContext): IO[Option[String]] = {
    if (isFavorite) performFavoriteAction(userID, videoID)
    else performUnfavoriteAction(userID, videoID)
  }

  private def performFavoriteAction(userID: Int, videoID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"User $userID is adding favorite for Video ID: $videoID"))
      favoriteExists <- readDBJsonOptional(
        s"SELECT 1 FROM ${schemaName}.feedback_detail_table WHERE user_id = ? AND video_id = ? AND favorite = true;",
        List(SqlParameter("Int", userID.toString), SqlParameter("Int", videoID.toString))
      ).map(_.isDefined)

      result <-
        if (favoriteExists) {
          IO(logger.info("Favorite already exists.")) >>
            IO.pure(Some("Favorite already exists"))
        } else {
          for {
            // Insert new favorite record
            _ <- writeDB(
              s"""INSERT INTO ${schemaName}.feedback_detail_table (user_id, video_id, like, favorite, timestamp)
                  VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP);""".stripMargin,
              List(
                SqlParameter("Int", userID.toString),
                SqlParameter("Int", videoID.toString),
                SqlParameter("Boolean", false.toString),
                SqlParameter("Boolean", true.toString)
              )
            )
            // Increment the favorites count in VideoInfoTable
            _ <- writeDB(
              s"UPDATE ${schemaName}.video_info_table SET favorites = favorites + 1 WHERE video_id = ?;",
              List(SqlParameter("Int", videoID.toString))
            )
          } yield None
        }
    } yield result
  }

  private def performUnfavoriteAction(userID: Int, videoID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"User $userID is removing favorite for Video ID: $videoID"))
      favoriteExists <- readDBJsonOptional(
        s"SELECT 1 FROM ${schemaName}.feedback_detail_table WHERE user_id = ? AND video_id = ? AND favorite = true;",
        List(SqlParameter("Int", userID.toString), SqlParameter("Int", videoID.toString))
      ).map(_.isDefined)

      result <-
        if (!favoriteExists) {
          IO(logger.info("Favorite record does not exist.")) >>
            IO.pure(Some("Favorite record does not exist"))
        } else {
          for {
            // Delete the favorite record
            _ <- writeDB(
              s"""DELETE FROM ${schemaName}.feedback_detail_table
                  WHERE user_id = ? AND video_id = ? AND favorite = true;""",
              List(SqlParameter("Int", userID.toString), SqlParameter("Int", videoID.toString))
            )
            // Decrement the favorites count in VideoInfoTable
            _ <- writeDB(
              s"UPDATE ${schemaName}.video_info_table SET favorites = favorites - 1 WHERE video_id = ?;",
              List(SqlParameter("Int", videoID.toString))
            )
          } yield None
        }
    } yield result
  }
}