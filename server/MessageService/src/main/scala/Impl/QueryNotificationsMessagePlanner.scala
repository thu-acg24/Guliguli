package Impl

import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.MessageService.Notification
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory


case class QueryNotificationsMessagePlanner(
                                        token: String,
                                        override val planContext: PlanContext
                                      ) extends Planner[List[Notification]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[Notification]] = {
    for {
      // Step 1: Validate token and get userID
      _ <- IO(logger.info(s"开始验证Token: $token"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: Query notifications if token is valid, else return an empty list
      _ <- IO(logger.info(s"Token验证通过, userID=$userID"))

      messages <- queryAndFormatMessages(userID)
    } yield messages
  }


  /**
   * 查询并格式化两个用户之间的消息记录
   *
   * @param userID 当前用户ID
   * @return 按时间戳降序排列的消息列表
   */
  private def queryAndFormatMessages(userID: Int)(using PlanContext): IO[List[Notification]] = {
    for {
      _ <- IO(logger.info(s"获取用户 $userID 的通知"))
      messages <- queryNotifications(userID)
      _ <- IO(logger.info(s"返回格式化的消息列表，共 ${messages.size} 条"))
    } yield messages
  }

  /**
   * 查询数据库中 userID 和 targetID 之间的私信
   *
   * @param userID   当前用户ID
   * @return 原始消息列表
   */
  private def queryNotifications(userID: Int)(using PlanContext): IO[List[Notification]] = {
    val sql =
      s"""
         |SELECT message_id, receiver_id, content, send_time
         |FROM $schemaName.message_table
         |WHERE receiver_id = ?
         |AND is_notification = TRUE
         |ORDER BY send_time DESC;
         """.stripMargin
    val parameters = List(
      SqlParameter("Int", userID.toString)
    )
    for {
      _ <- IO(logger.info(s"生成查询通知记录的SQL: $sql"))
      rows <- readDBRows(sql, parameters)
      messages <- IO(rows.map(decodeMessage))
    } yield messages
  }

  /**
   * 将 SQL 查询结果解析为 Message 对象
   *
   * @param json SQL 查询返回的单条记录 JSON
   * @return 转换后的 Message 对象
   */
  private def decodeMessage(json: Json): Notification = {
    Notification(
      messageID = decodeField[Int](json, "message_id"),
      content = decodeField[String](json, "content"),
      timestamp = decodeField[DateTime](json, "send_time")
    )
  }
}