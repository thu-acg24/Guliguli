package Impl

import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.MessageService.ReplyNotice
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory


case class QueryReplyNoticesMessagePlanner(
                                        token: String,
                                        override val planContext: PlanContext
                                      ) extends Planner[List[ReplyNotice]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[ReplyNotice]] = {
    for {
      // Step 1: Validate token and get userID
      _ <- IO(logger.info(s"开始验证Token: $token"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: Query notices
      _ <- IO(logger.info(s"Token验证通过, userID=$userID"))

      notices <- queryAndFormatNotices(userID)
      _ <- readReplyNotices(userID)
    } yield notices
  }


  /**
   * 查询并格式化通知记录
   *
   * @param userID 当前用户ID
   * @return 按时间戳降序排列的消息列表
   */
  private def queryAndFormatNotices(userID: Int)(using PlanContext): IO[List[ReplyNotice]] = {
    for {
      _ <- IO(logger.info(s"获取用户 $userID 的被回复通知"))
      notices <- queryNotices(userID)
      _ <- IO(logger.info(s"返回格式化的通知列表，共 ${notices.size} 条"))
    } yield notices
  }

  /**
   * 查询数据库中 userID 的回复通知
   *
   * @param userID   当前用户ID
   * @return 原始消息列表
   */
  private def queryNotices(userID: Int)(using PlanContext): IO[List[ReplyNotice]] = {
    val sql =
      s"""
         |SELECT notice_id, sender_id, content,
         |comment_id, original_content, original_comment_id, video_id, send_time
         |FROM $schemaName.reply_notice_table
         |WHERE receiver_id = ?
         |ORDER BY send_time DESC;
         """.stripMargin
    val parameters = List(
      SqlParameter("Int", userID.toString)
    )
    for {
      _ <- IO(logger.info(s"生成查询通知记录的SQL: $sql"))
      rows <- readDBRows(sql, parameters)
      notices <- IO(rows.map(decodeNotice))
    } yield notices
  }

  /**
   * 将 userID 的回复通知全部设为已读
   *
   * @param userID 当前用户ID
   * @return 原始消息列表
   */
  private def readReplyNotices(userID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
         |UPDATE $schemaName.reply_notice_table
         |SET unread = FALSE
         |WHERE
         |    (receiver_id = ?)
         |    AND unread = TRUE
          """.stripMargin
    val parameters = List(
      SqlParameter("Int", userID.toString)
    )
    for {
      _ <- IO(logger.info(s"生成修改未读评论的SQL: $sql"))
      _ <- writeDB(sql, parameters)
    } yield ()
  }

  /**
   * 将 SQL 查询结果解析为 ReplyNotice 对象
   *
   * @param json SQL 查询返回的单条记录 JSON
   * @return 转换后的 ReplyNotice 对象
   */
  private def decodeNotice(json: Json): ReplyNotice = {
    ReplyNotice(
      noticeID = decodeField[Int](json, "notice_id"),
      senderID = decodeField[Int](json, "sender_id"),
      content = decodeField[String](json, "content"),
      commentID = decodeField[Int](json, "comment_id"),
      originalContent = decodeField[String](json, "original_content"),
      originalCommentID = decodeField[Int](json, "original_comment_id"),
      videoID = decodeField[Int](json, "video_id"),
      timestamp = decodeField[DateTime](json, "send_time"),
    )
  }
}