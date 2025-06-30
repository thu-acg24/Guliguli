package Impl


import APIs.CommentService.QueryCommentByIDMessage
import APIs.UserService.GetUIDByTokenMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.CommentService.Comment
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class PublishCommentMessagePlanner(
                                         token: String,
                                         videoID: Int,
                                         commentContent: String,
                                         replyToCommentID: Option[Int],
                                         override val planContext: PlanContext
                                       ) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: 校验token是否有效并获取用户ID
      _ <- IO(logger.info(s"校验token是否有效: token=${token}"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: 校验视频是否存在
      _ <- IO(logger.info(s"校验视频是否存在: videoID=${videoID}"))
      _ <- QueryVideoInfoMessage(None, videoID).send

      // Step 3: 校验回复的目标评论是否存在（如果存在replyToCommentID）
      _ <- IO(logger.info(s"校验目标评论是否存在: replyToCommentID=${replyToCommentID}"))
      _ <- validateTargetComment(replyToCommentID)

      // Step 4: 检查评论内容是否符合要求
      _ <- IO(logger.info(s"校验评论内容是否合法: commentContent(长度)=${commentContent.length}"))
      _ <- validateCommentContent(commentContent)

      // Step 5: 组装评论数据并存储到数据库
      _ <- IO(logger.info("组装数据并插入到数据库"))
      _ <- insertComment(userID, videoID, commentContent, replyToCommentID)
    } yield ()
  }

  private def validateTargetComment(replyToCommentID: Option[Int])(using PlanContext): IO[Unit] = {
    replyToCommentID match {
      case Some(commentID) =>
        QueryCommentByIDMessage(commentID).send.flatMap {
          _ => IO.unit
        }
      case None => IO.unit
    }
  }

  private def validateCommentContent(commentContent: String)(using PlanContext): IO[Unit] = {
    if (commentContent.isEmpty || commentContent.length > 500) {
      IO {
        logger.error(s"无效的评论内容: commentContent='${commentContent}' 长度=${commentContent.length}")
      } >> IO.raiseError(new IllegalArgumentException("Invalid Comment Content"))
    } else {
      IO.unit
    }
  }

  private def insertComment(
                             userID: Int,
                             videoID: Int,
                             commentContent: String,
                             replyToCommentID: Option[Int]
                           )(using PlanContext): IO[String] = {
    val timestamp = DateTime.now()
    val sql =
      s"""
         |INSERT INTO ${schemaName}.comment_table
         |  (content, video_id, author_id, reply_to_id, likes, timestamp)
         |VALUES
         |  (?, ?, ?, ?, 0, ?);
         |""".stripMargin

    writeDB(
      sql,
      List(
        SqlParameter("String", commentContent),
        SqlParameter("Int", videoID.toString),
        SqlParameter("Int", userID.toString),
        SqlParameter("Int", replyToCommentID.map(_.toString).getOrElse("null")),
        SqlParameter("DateTime", timestamp.getMillis.toString)
      )
    )
  }
}