package Impl

import APIs.CommentService.QueryCommentByIDMessage
import APIs.UserService.{GetUIDByTokenMessage, QueryUserInfoMessage, QueryUserRoleMessage}
import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.UserService.{UserInfo, UserRole}
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class SendReplyNoticeMessagePlanner(
    token: String,
    commentID: Int,
    override val planContext: PlanContext
) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: 验证调用者Token的合法性
      _ <- IO(logger.info(s"开始验证调用者Token: ${token}"))
      userID <- GetUIDByTokenMessage(token).send
      _ <- IO(logger.info(s"验证成功，用户: ${userID}"))
      // Step 2: 检查Comment是否存在
      comment <- QueryCommentByIDMessage(commentID).send
      replyID <- IO {
        comment.replyToID match {
          case Some(id) => id
          case None => throw InvalidInputException("评论不存在被回复者")
        }
      }
      _ <- if comment.authorID == userID then IO.unit else IO.raiseError(InvalidInputException("用户不是评论发布者"))
      replyComment <- QueryCommentByIDMessage(replyID).send
      receiverID = replyComment.authorID
      videoID = replyComment.videoID
      // Step 3: 检测评论是否存在
      _ <- IO(logger.info("检查评论是否存在"))
      _ <- checkRecord(commentID)
      // Step 3: 插入数据库
      _ <- IO(logger.info("消息构造成功，开始插入数据库"))
      timestamp <- IO(DateTime.now())
      _ <- insertRecord(userID, receiverID, comment.content, commentID,
        replyComment.content, replyComment.authorID, videoID, timestamp)
    } yield()
  }

  private def checkRecord(commentID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
         SELECT COUNT(*) from ${schemaName}.reply_notice_table WHERE comment_id = ?;
       """
    for {
      count <- readDBInt(sql, List(SqlParameter("Int", commentID.toString)))
      _ <- if (count <= 0) IO.unit else IO.raiseError(InvalidInputException("未查找到评论"))
    } yield()
  }

  private def insertRecord(senderID: Int, receiverID: Int, content: String, commentID: Int,
                           originalContent: String, originalCommentID: Int, videoID: Int, timestamp: DateTime)
                          (using PlanContext): IO[Unit] = {
    val sql =
      s"""
         INSERT INTO ${schemaName}.reply_notice_table
         (sender_id, receiver_id, content, comment_id, original_content, original_comment_id, video_id, send_time)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?);
       """

    writeDB(
      sql,
      List(
        SqlParameter("Int", senderID.toString),
        SqlParameter("Int", receiverID.toString),
        SqlParameter("String", content),
        SqlParameter("Int", commentID.toString),
        SqlParameter("String", originalContent),
        SqlParameter("Int", originalCommentID.toString),
        SqlParameter("Int", videoID.toString),
        SqlParameter("DateTime", timestamp.getMillis.toString),
      )
    ).as(())
  }
}