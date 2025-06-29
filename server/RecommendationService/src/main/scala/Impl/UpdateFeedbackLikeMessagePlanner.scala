package Impl


import Objects.VideoService.VideoStatus
import Objects.VideoService.Video
import APIs.VideoService.QueryVideoInfoMessage
import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits._
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
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class UpdateFeedbackLikeMessagePlanner(
                                             token: String,
                                             videoID: Int,
                                             isLike: Boolean,
                                             override val planContext: PlanContext
                                           ) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Validate Token
      _ <- IO(logger.info(s"[Step 1] 校验Token的合法性: token=${token}"))
      userIDOpt <- validateToken()
      result <- userIDOpt match {
        case None =>
          IO(logger.warn(s"[Step 1.1] Token无效")) *> IO.pure(Some("Invalid Token"))
        case Some(userID) =>
          for {
            // Step 2: Validate Video
            _ <- IO(logger.info(s"[Step 2] 检查视频是否存在: videoID=${videoID}"))
            videoOpt <- validateVideo()
            r <- videoOpt match {
              case None =>
                IO(logger.warn(s"[Step 2.1] 视频未找到: videoID=${videoID}")) *> IO.pure(Some("Video Not Found"))
              case Some(_) =>
                // Step 3: Perform Like/Unlike action
                _ <- IO(logger.info(s"[Step 3] 根据isLike=${isLike}执行点赞或取消操作"))
                performLikeAction(userID)
            }
          } yield r
      }
    } yield result
  }

  private def validateToken()(using PlanContext): IO[Option[Int]] = {
    for {
      _ <- IO(logger.info(s"[validateToken] 调用getUIDByTokenMessage(${token})"))
      userIDOpt <- GetUIDByTokenMessage(token).send
      _ <- IO(logger.info(s"[validateToken] 校验结果: ${userIDOpt.map(id => s"有效的 userID=${id}").getOrElse("Token 无效")}"))
    } yield userIDOpt
  }

  private def validateVideo()(using PlanContext): IO[Option[Video]] = {
    for {
      _ <- IO(logger.info(s"[validateVideo] 调用QueryVideoInfoMessage(None, ${videoID})"))
      videoOpt <- QueryVideoInfoMessage(None, videoID).send
      _ <- IO(logger.info(
        s"[validateVideo] 视频校验结果: ${
          videoOpt.map(v => s"视频存在, videoID=${v.videoID}").getOrElse("视频不存在")
        }"
      ))
    } yield videoOpt
  }

  private def performLikeAction(userID: Int)(using PlanContext): IO[Option[String]] = {
    if (isLike) performLike(userID) else performUnlike(userID)
  }

  private def performLike(userID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      // Check if like record exists
      _ <- IO(logger.info(s"[performLike] 检查用户(${userID})是否已对视频(${videoID})点赞"))
      feedbackExist <- checkFeedbackExist(userID)
      result <- if (feedbackExist) {
        IO(logger.warn("[performLike] 点赞记录已存在")) *> IO.pure(Some("Like already exists"))
      } else {
        // Insert like record
        _ <- IO(logger.info("[performLike] 插入点赞记录"))
        _ <- insertLikeRecord(userID)
        // Update video likes count
        _ <- IO(logger.info("[performLike] 更新视频点赞计数 +1"))
        updateVideoLikes(increment = true).as(None)
      }
    } yield result
  }

  private def performUnlike(userID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      // Check if like record exists
      _ <- IO(logger.info(s"[performUnlike] 检查用户(${userID})的点赞记录是否存在"))
      feedbackExist <- checkFeedbackExist(userID)
      result <- if (!feedbackExist) {
        IO(logger.warn("[performUnlike] 点赞记录不存在")) *> IO.pure(Some("Like record does not exist"))
      } else {
        // Delete like record
        _ <- IO(logger.info("[performUnlike] 删除点赞记录"))
        _ <- deleteLikeRecord(userID)
        // Update video likes count
        _ <- IO(logger.info("[performUnlike] 更新视频点赞计数 -1"))
        updateVideoLikes(increment = false).as(None)
      }
    } yield result
  }

  private def checkFeedbackExist(userID: Int)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
SELECT COUNT(*) > 0 FROM "${schemaName}".feedback_detail_table
WHERE user_id = ? AND video_id = ? AND like = true;
       """.stripMargin
    readDBBoolean(sql, List(SqlParameter("Int", userID.toString), SqlParameter("Int", videoID.toString)))
  }

  private def insertLikeRecord(userID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
INSERT INTO "${schemaName}".feedback_detail_table (user_id, video_id, like, favorite, timestamp)
VALUES (?, ?, true, false, ?);
       """.stripMargin
    val timestamp = DateTime.now()
    writeDB(sql, List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString),
      SqlParameter("DateTime", timestamp.getMillis.toString)
    )).void
  }

  private def deleteLikeRecord(userID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
DELETE FROM "${schemaName}".feedback_detail_table
WHERE user_id = ? AND video_id = ? AND like = true;
       """.stripMargin
    writeDB(sql, List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString)
    )).void
  }

  private def updateVideoLikes(increment: Boolean)(using PlanContext): IO[Unit] = {
    val operation = if (increment) "+" else "-"
    val sql =
      s"""
UPDATE "${schemaName}".video_info_table
SET likes = likes $operation 1
WHERE video_id = ?;
       """.stripMargin
    writeDB(sql, List(SqlParameter("Int", videoID.toString))).void
  }
}