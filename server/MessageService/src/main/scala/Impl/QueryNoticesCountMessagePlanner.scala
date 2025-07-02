  package Impl

import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.MessageService.NoticesCount
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory


case class QueryNoticesCountMessagePlanner(
                                        token: String,
                                        override val planContext: PlanContext
                                      ) extends Planner[NoticesCount] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[NoticesCount] = {
    val messageSql =
      s"""
         |SELECT COUNT(*) AS unread_count
         |FROM $schemaName.message_table
         |WHERE receiver_id = ?
         |AND unread = True
               """.stripMargin
    val notificationSql =
      s"""
         |SELECT COUNT(*) AS unread_count
         |FROM $schemaName.notification_table
         |WHERE receiver_id = ?
         |AND unread = True
               """.stripMargin
    val replyNoticeSql =
      s"""
         |SELECT COUNT(*) AS unread_count
         |FROM $schemaName.reply_notice_table
         |WHERE receiver_id = ?
         |AND unread = True
               """.stripMargin
    for {
      // Step 1: Validate token and get userID
      _ <- IO(logger.info(s"开始验证Token: $token"))
      userID <- GetUIDByTokenMessage(token).send

      _ <- IO(logger.info(s"Token验证通过, userID=$userID"))
      parameter <- IO(List(SqlParameter("Int", userID.toString)))
      messageCount <- readDBInt(messageSql, parameter)
      notificationCount <- readDBInt(notificationSql, parameter)
      replyNoticeCount <- readDBInt(replyNoticeSql, parameter)
      result <- IO(NoticesCount(
        messageCount,
        notificationCount,
        replyNoticeCount
      ))
    } yield result
  }
}