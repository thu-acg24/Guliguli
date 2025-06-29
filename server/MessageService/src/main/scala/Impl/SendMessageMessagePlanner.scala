package Impl

import APIs.UserService.{QueryUserInfoMessage, QueryUserRoleMessage, GetUIDByTokenMessage}
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
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.UserService.{UserRole, UserInfo}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class SendMessageMessagePlanner(
    token: String,
    receiverID: Int,
    messageContent: String,
    isNotification: Boolean,
    override val planContext: PlanContext
) extends Planner[Option[String]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: 验证调用者Token的合法性
      _ <- IO(logger.info(s"开始验证调用者Token: ${token}"))
      validatedUserIDOptional <- validateToken()
      _ <- IO(logger.info(s"Token验证结果: ${validatedUserIDOptional.getOrElse("无效Token")}"))
      
      result <- validatedUserIDOptional match {
        case Some(userID) =>
          // Step 2: 如果是通知，检查权限是否合法
          for {
            _ <- IO(logger.info("开始校验通知的合法性"))
            notificationCheckResult <- checkNotificationPermission(userID)
            _ <- IO(logger.info(s"通知权限校验结果: ${notificationCheckResult.getOrElse("权限正常")}"))
            
            finalResult <- notificationCheckResult match {
              case None =>
                // Step 3: 校验接收方是否合法
                for {
                  _ <- IO(logger.info(s"验证接收方ID: ${receiverID}是否存在"))
                  receiverCheckResult <- validateReceiver()
                  _ <- IO(logger.info(s"接收方校验结果: ${receiverCheckResult.getOrElse("接收方有效")}"))
                  
                  actualResult <- receiverCheckResult match {
                    case None =>
                      // Step 4: 构造消息并插入数据库
                      for {
                        constructedMessage <- constructMessage(userID)
                        _ <- IO(logger.info("消息构造成功，开始插入数据库"))
                        insertResult <- insertMessageRecord(constructedMessage)
                      } yield insertResult
                    case errorResult => IO(errorResult)
                  }
                } yield actualResult
              case errorResult => IO(errorResult)
            }
          } yield finalResult

        case None =>
          IO(Some("Invalid Token")) // 如果验证Token失败，直接返回
      }
    } yield result
  }

  private def validateToken()(using PlanContext): IO[Option[Int]] = {
    GetUIDByTokenMessage(token).send
  }

  private def checkNotificationPermission(senderID: Int)(using PlanContext): IO[Option[String]] = {
    if (isNotification) {
      for {
        userRoleOpt <- QueryUserRoleMessage(token).send
        result <- userRoleOpt match {
          case Some(UserRole.Auditor) => IO(None) // 审核员权限允许发送通知
          case _ => IO(Some("Invalid Permission")) // 无权限
        }
      } yield result
    } else {
      IO(None) // 如果不是通知，无需校验权限
    }
  }

  private def validateReceiver()(using PlanContext): IO[Option[String]] = {
    QueryUserInfoMessage(receiverID).send.map {
      case Some(_) => None // 接收方存在
      case None => Some("Receiver Not Found") // 接收方无效
    }
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

  private def insertMessageRecord(message: Message)(using PlanContext): IO[Option[String]] = {
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
    ).map(_ => None).handleErrorWith { error =>
      IO(logger.error(s"消息插入失败，原因: ${error.getMessage}")) *> IO(Some("Unable to send message"))
    }
  }

  private case class Message(
      senderID: Int,
      receiverID: Int,
      content: String,
      timestamp: DateTime,
      isNotification: Boolean
  )
}