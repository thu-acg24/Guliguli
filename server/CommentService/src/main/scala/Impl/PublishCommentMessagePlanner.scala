package Impl


import Objects.VideoService.VideoStatus
import Objects.CommentService.Comment
import Objects.VideoService.Video
import APIs.CommentService.QueryCommentByIDMessage
import APIs.VideoService.QueryVideoInfoMessage
import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import cats.implicits.*
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
import APIs.UserService.getUIDByTokenMessage
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class PublishCommentMessagePlanner(
                                         token: String,
                                         videoID: Int,
                                         commentContent: String,
                                         replyToCommentID: Option[Int],
                                         override val planContext: PlanContext
                                       ) extends Planner[Option[String]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: 校验token是否有效并获取用户ID
      _ <- IO(logger.info(s"校验token是否有效: token=${token}"))
      userIDOpt <- getUIDByTokenMessage(token).send
      userID <- validateToken(userIDOpt)

      // Step 2: 校验视频是否存在
      _ <- IO(logger.info(s"校验视频是否存在: videoID=${videoID}"))
      videoOpt <- QueryVideoInfoMessage(None, videoID).send
      _ <- validateVideo(videoOpt)

      // Step 3: 校验回复的目标评论是否存在（如果存在replyToCommentID）
      _ <- IO(logger.info(s"校验目标评论是否存在: replyToCommentID=${replyToCommentID}"))
      _ <- validateTargetComment(replyToCommentID)

      // Step 4: 检查评论内容是否符合要求
      _ <- IO(logger.info(s"校验评论内容是否合法: commentContent(长度)=${commentContent.length}"))
      _ <- validateCommentContent(commentContent)

      // Step 5: 组装评论数据并存储到数据库
      _ <- IO(logger.info("组装数据并插入到数据库"))
      insertionResult <- insertComment(userID, videoID, commentContent, replyToCommentID)
    } yield insertionResult
  }

  private def validateToken(userIDOpt: Option[Int])(using PlanContext): IO[Int] = {
    userIDOpt match {
      case Some(userID) => IO.pure(userID)
      case None =>
        IO {
          logger.error(s"无效的token: ${token}")
        } >> IO.raiseError(new IllegalArgumentException("Invalid Token"))
    }
  }

  private def validateVideo(videoOpt: Option[Video])(using PlanContext): IO[Unit] = {
    videoOpt match {
      case Some(_) => IO.unit
      case None =>
        IO {
          logger.error(s"视频ID不存在: videoID=${videoID}")
        } >> IO.raiseError(new IllegalArgumentException("Video Not Found"))
    }
  }

  private def validateTargetComment(replyToCommentID: Option[Int])(using PlanContext): IO[Unit] = {
    replyToCommentID match {
      case Some(commentID) =>
        QueryCommentByIDMessage(commentID).send.flatMap {
          case Some(_) => IO.unit
          case None =>
            IO {
              logger.error(s"目标评论ID不存在: replyToCommentID=${commentID}")
            } >> IO.raiseError(new IllegalArgumentException("Target Comment Not Found"))
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
                           )(using PlanContext): IO[Option[String]] = {
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
    ).map {
      case "Operation(s) done successfully" =>
        None
      case _ =>
        logger.error(s"评论插入失败: userID=${userID}, videoID=${videoID}, replyToCommentID=${replyToCommentID}")
        Some("Failed to publish comment")
    }
  }
}