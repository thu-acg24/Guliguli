package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.APIException.InvalidInputException
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
    override val planContext: PlanContext
) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: 验证调用者Token的合法性
      _ <- IO(logger.info(s"开始验证调用者Token: ${token}"))
      userID <- GetUIDByTokenMessage(token).send
      _ <- IO(logger.info(s"验证成功，用户: ${userID}"))
      _ <- IO(logger.info(s"验证接收方ID: ${receiverID}是否存在"))
      _ <- QueryUserInfoMessage(receiverID).send
      _ <- IO(logger.info(s"接收方有效"))
      _ <- IO(logger.info("开始插入数据库"))
      _ <- insertMessageRecord(userID)
    }yield()
  }

  private def insertMessageRecord(senderID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
         INSERT INTO ${schemaName}.message_table
         (sender_id, receiver_id, content, send_time)
         VALUES (?, ?, ?, ?);
       """

    writeDB(
      sql,
      List(
        SqlParameter("Int", senderID.toString),
        SqlParameter("Int", receiverID.toString),
        SqlParameter("String", messageContent),
        SqlParameter("DateTime", DateTime.now.getMillis.toString)
      )
    ).as(())
  }
}