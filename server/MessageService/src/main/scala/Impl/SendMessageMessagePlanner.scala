package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserInfoMessage
import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserInfo
import Objects.UserService.UserRole
import cats.effect.IO
import cats.implicits.*
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class SendMessageMessagePlanner(
    token: String,
    receiverID: Int,
    messageContent: String,
    isNotification: Boolean,
    override val planContext: PlanContext
) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: 验证调用者Token的合法性
      _ <- IO(logger.info(s"开始验证调用者Token: ${token}"))
      userID <- validateToken()
      _ <- IO(logger.info(s"验证成功，用户: ${userID}"))
      _ <- IO(logger.info("开始校验通知的合法性"))
      _ <- checkNotificationPermission(userID)
      _ <- IO(logger.info("权限正常"))
      _ <- IO(logger.info(s"验证接收方ID: ${receiverID}是否存在"))
      _ <- validateReceiver()
      _ <- IO(logger.info(s"接收方有效"))
      constructedMessage <- constructMessage(userID)
      _ <- IO(logger.info("消息构造成功，开始插入数据库"))
      _ <- insertMessageRecord(constructedMessage)
    }yield()
  }

  private def validateToken()(using PlanContext): IO[Int] = {
    GetUIDByTokenMessage(token).send
  }

  private def checkNotificationPermission(senderID: Int)(using PlanContext): IO[Unit] = {
    if (isNotification) {
      for {
        userRole <- QueryUserRoleMessage(token).send
        _ <- userRole match {
          case UserRole.Auditor => IO.unit // 审核员权限允许发送通知
          case _ => IO.raiseError(IllegalArgumentException("Invalid Permission"))
        }
      } yield ()
    } else {
      IO.unit // 如果不是通知，无需校验权限
    }
  }

  private def validateReceiver()(using PlanContext): IO[Unit] = {
    QueryUserInfoMessage(receiverID).send.as{()}
  }

  private def constructMessage(senderID: Int)(using PlanContext): IO[Message] = {
    val messageSenderID = if (isNotification) 0 else senderID
    val timestamp = DateTime.now
    val message = Message(
      senderID = messageSenderID,
      receiverID = receiverID,
      content = messageContent,
      timestamp = timestamp,
      isNotification = isNotification
    )
    IO(message)
  }

  private def insertMessageRecord(message: Message)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
         INSERT INTO ${schemaName}.message_table
         (sender_id, receiver_id, content, timestamp, is_notification)
         VALUES (?, ?, ?, ?, ?);
       """

    writeDB(
      sql,
      List(
        SqlParameter("Int", message.senderID.toString),
        SqlParameter("Int", message.receiverID.toString),
        SqlParameter("String", message.content),
        SqlParameter("DateTime", message.timestamp.getMillis.toString),
        SqlParameter("Boolean", message.isNotification.toString)
      )
    ).as(())
  }

  private case class Message(
      senderID: Int,
      receiverID: Int,
      content: String,
      timestamp: DateTime,
      isNotification: Boolean
  )
}