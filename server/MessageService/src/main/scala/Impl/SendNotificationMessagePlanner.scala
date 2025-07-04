package Impl

import APIs.UserService.{GetUIDByTokenMessage, QueryUserInfoMessage, QueryUserRoleMessage}
import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.UserService.{UserInfo, UserRole}
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class SendNotificationMessagePlanner(
    token: String,
    receiverID: Int,
    title: String,
    messageContent: String,
    override val planContext: PlanContext
) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: 验证调用者Token的合法性
      _ <- IO(logger.info("开始校验用户权限"))
      userRole <- QueryUserRoleMessage(token).send
      _ <- userRole match {
        case UserRole.Admin | UserRole.Auditor => IO.unit // 审核员权限允许发送通知
        case _ => IO.raiseError(InvalidInputException("Invalid Permission"))
      }
      _ <- IO(logger.info("权限正常"))
      _ <- IO(logger.info(s"验证接收方ID: ${receiverID}是否存在"))
      _ <- QueryUserInfoMessage(receiverID).send
      _ <- IO(logger.info(s"接收方有效"))
      _ <- insertMessageRecord()
    }yield()
  }

  private def insertMessageRecord()(using PlanContext): IO[Unit] = {
    val sql =
      s"""
           INSERT INTO ${schemaName}.notification_table
           (receiver_id, title, content, send_time)
           VALUES (?, ?, ?, ?);
         """

    writeDB(
      sql,
      List(
        SqlParameter("Int", receiverID.toString),
        SqlParameter("String", title),
        SqlParameter("String", messageContent),
        SqlParameter("DateTime", DateTime.now.getMillis.toString)
      )
    ).as(())
  }
}