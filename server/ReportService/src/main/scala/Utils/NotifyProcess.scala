package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import Objects.MessageService.Message
import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import cats.effect.IO
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.API.{PlanContext}

case object NotifyProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  def sendNotification(token: String, reporterID: Int, messageContent: String)(using PlanContext): IO[Option[String]] = {
  // val logger = LoggerFactory.getLogger("sendNotification")  // 同文后端处理: logger 统一
  
    for {
      _ <- IO(logger.info(s"开始校验reporterID: ${reporterID}"))
  
      // Step 1: 验证reporterID是否存在于user_table
      reporterExists <- readDBJsonOptional(
        s"SELECT user_id FROM ${schemaName}.user_table WHERE user_id = ?;",
        List(SqlParameter("Int", reporterID.toString))
      )
      
      result <- if (reporterExists.isEmpty) {
        // 如果reporterID无效，则直接返回“无效的举报人ID”
        IO(logger.error(s"校验失败，无效的reporterID: ${reporterID}")) *>
        IO.pure(Some("无效的举报人ID"))
      } else {
        for {
          _ <- IO(logger.info(s"用户ID ${reporterID} 校验成功"))
  
          // Step 2: 构造Message对象
          message <- IO {
            val timestamp = DateTime.now
            Message(
              messageID = 0, // messageID将在写入数据库时由数据库自动生成
              senderID = 0, // 发送者ID设为系统0
              receiverID = reporterID,
              content = messageContent,
              timestamp = timestamp,
              isNotification = true
            )
          }
          _ <- IO(logger.info(s"构造Message对象: ${message}"))
  
          // Step 2.2: 写入message_table
          insertResult <- writeDB(
            s"""
            INSERT INTO ${schemaName}.message_table
            (sender_id, receiver_id, content, timestamp, is_notification)
            VALUES (?, ?, ?, ?, ?);
            """,
            List(
              SqlParameter("Int", message.senderID.toString),
              SqlParameter("Int", message.receiverID.toString),
              SqlParameter("String", message.content),
              SqlParameter("DateTime", message.timestamp.getMillis.toString),
              SqlParameter("Boolean", message.isNotification.toString)
            )
          )
          _ <- IO(logger.info(s"消息插入数据库成功，返回结果: ${insertResult}"))
        } yield None
      }
    } yield result
  }
}
