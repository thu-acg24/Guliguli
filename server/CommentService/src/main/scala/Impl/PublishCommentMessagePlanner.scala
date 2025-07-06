package Impl


import APIs.CommentService.QueryCommentByIDMessage
import APIs.MessageService.SendReplyNoticeMessage
import Common.APIException.InvalidInputException
import APIs.UserService.GetUIDByTokenMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.CommentService.Comment
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits._
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class PublishCommentMessagePlanner(
                                         token: String,
                                         videoID: Int,
                                         commentContent: String,
                                         replyToCommentID: Option[Int],
                                         override val planContext: PlanContext
                                       ) extends Planner[Comment] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Comment] = {
    for {
      // Step 1: 校验token是否有效并获取用户ID
      _ <- IO(logger.info(s"校验token是否有效: token=${token}"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: 校验视频是否存在
      _ <- IO(logger.info(s"校验视频是否存在: videoID=${videoID}"))
      _ <- QueryVideoInfoMessage(None, videoID).send

      // Step 3: 检查评论内容是否符合要求
      _ <- IO(logger.info(s"校验评论内容是否合法: commentContent(长度)=${commentContent.length}"))
      _ <- validateCommentContent(commentContent)

      // Step 4: 校验回复的目标评论是否存在（如果存在replyToCommentID 并提取 rootID）
      _ <- IO(logger.info(s"校验目标评论是否存在: replyToCommentID=${replyToCommentID}"))
      _ <- replyToCommentID match {
        case Some(id) => checkCommentExists(id)
        case None => IO.unit
      }
      rootID <- validateTargetComment(replyToCommentID)

      // Step 5: 组装评论数据并存储到数据库
      _ <- IO(logger.info("组装数据并插入到数据库"))
      (timeStamp, curID) <- insertComment(userID, videoID, commentContent, replyToCommentID, rootID)

      // Step 6: 如果是回复，发送通知并增加所属楼层回复数
      _ <- replyToCommentID match {
        case None => IO.unit
        case Some(_) =>
          writeDB(s"UPDATE ${schemaName}.comment_table SET reply_count = reply_count + 1 WHERE comment_id = ?",
            List(SqlParameter("Int", rootID.toString))) >> SendReplyNoticeMessage(token, curID).send
      }
    } yield Comment(curID, commentContent, videoID, userID, replyToCommentID, 0, 0, timeStamp)
  }

  /**
   * 检查commentID是否存在
   * @param commentID 评论ID
   * @return Boolean，存在返回true，不存在返回false
   */
  private def checkCommentExists(commentID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
         |SELECT COUNT(*) FROM ${schemaName}.comment_table
         |WHERE comment_id = ?;
       """.stripMargin
    readDBBoolean(sql, List(SqlParameter("Int", commentID.toString)))
      .ensure(InvalidInputException("回复不存在"))(x => !x).void
  }

  private def validateTargetComment(replyToCommentID: Option[Int])(using PlanContext): IO[Option[Int]] = {
    val sql = s"""
         |SELECT root_id FROM ${schemaName}.comment_table
         |WHERE comment_id = ?;
         |""".stripMargin
    replyToCommentID match {
      case Some(commentID) => readDBJson(sql, List(SqlParameter("Int", commentID.toString))).map {
        json => decodeField[Option[Int]](json, "root_id")
      }
      case None => IO.pure(None)
    }
  }

  private def validateCommentContent(commentContent: String)(using PlanContext): IO[Unit] = {
    if (commentContent.isEmpty || commentContent.length > 500) {
      IO {
        logger.error(s"无效的评论内容: commentContent='${commentContent}' 长度=${commentContent.length}")
      } >> IO.raiseError(InvalidInputException("Invalid Comment Content"))
    } else {
      IO.unit
    }
  }

  private def insertComment(
                             userID: Int,
                             videoID: Int,
                             commentContent: String,
                             replyToCommentIDOpt: Option[Int],
                             rootIDOpt: Option[Int],
                           )(using PlanContext): IO[(DateTime, Int)] = {
    for {
      timeStamp <- IO(DateTime.now())
      commentID <- (replyToCommentIDOpt, rootIDOpt) match {
        case (Some(replyToCommentID), Some(rootID)) =>
          readDBInt(
            s"""
               |INSERT INTO ${schemaName}.comment_table
               |  (content, video_id, author_id, reply_to_id, root_id, likes, time_stamp)
               |VALUES (?, ?, ?, ?, ?, 0, ?)
               |RETURNING comment_id;
               |""".stripMargin,
            List(
              SqlParameter("String", commentContent),
              SqlParameter("Int", videoID.toString),
              SqlParameter("Int", userID.toString),
              SqlParameter("int", replyToCommentID.toString),
              SqlParameter("int", rootID.toString),
              SqlParameter("DateTime", timeStamp.getMillis.toString)
            ))
        case (None, None) =>
          readDBInt(
            s"""
               |INSERT INTO ${schemaName}.comment_table
               |  (content, video_id, author_id, likes, time_stamp)
               |VALUES (?, ?, ?, 0, ?)
               |RETURNING comment_id;
               |""".stripMargin,
            List(
              SqlParameter("String", commentContent),
              SqlParameter("Int", videoID.toString),
              SqlParameter("Int", userID.toString),
              SqlParameter("DateTime", timeStamp.getMillis.toString)
            ))
        case _ =>
          IO.raiseError(InvalidInputException("replyToCommentID和rootID必须同时为空或同时不为空"))
      }
    } yield (timeStamp, commentID)
  }
}