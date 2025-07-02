package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.MessageService.Message
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory


case class QueryMessagesMessagePlanner(
                                        token: String,
                                        targetID: Int,
                                        override val planContext: PlanContext
                                      ) extends Planner[List[Message]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[Message]] = {
    for {
      // Step 1: Validate token and get userID
      _ <- IO(logger.info(s"开始验证Token: $token"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: Query messages if token is valid, else return an empty list
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
  private def queryAndFormatMessages(userID: Int)(using PlanContext): IO[List[Message]] = {
    for {
      _ <- IO(logger.info(s"获取用户 $userID 和目标用户 $targetID 的私信记录"))
      messages <- queryMessagesBetweenUsers(userID, targetID)
      _ <- IO(logger.info(s"返回格式化的消息列表，共 ${messages.size} 条"))
    } yield messages
  }

  /**
   * 查询数据库中 userID 和 targetID 之间的私信
   *
   * @param userID   当前用户ID
   * @param targetID 目标用户ID
   * @return 原始消息列表
   */
  private def queryMessagesBetweenUsers(userID: Int, targetID: Int)(using PlanContext): IO[List[Message]] = {
    val sql =
      s"""
         |SELECT message_id, sender_id, receiver_id, content, send_time
         |FROM $schemaName.message_table
         |WHERE ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?))
         |ORDER BY send_time DESC;
         """.stripMargin
    val parameters = List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", targetID.toString),
      SqlParameter("Int", targetID.toString),
      SqlParameter("Int", userID.toString)
    )
    for {
      _ <- IO(logger.info(s"生成查询私信记录的SQL: $sql"))
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
  private def decodeMessage(json: Json): Message = {
    Message(
      messageID = decodeField[Int](json, "message_id"),
      senderID = decodeField[Int](json, "sender_id"),
      content = decodeField[String](json, "content"),
      timestamp = decodeField[DateTime](json, "send_time")
    )
  }
}