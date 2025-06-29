package Impl


import APIs.DanmakuService.QueryDanmakuByIDMessage
import APIs.MessageService.SendMessageMessage
import Utils.NotifyProcess.sendNotification
import Objects.DanmakuService.Danmaku
import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe.Json
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits._
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
import APIs.UserService.GetUIDByTokenMessage
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ReportDanmakuContentMessagePlanner(
    token: String,
    danmakuID: Int,
    reason: String,
    override val planContext: PlanContext
) extends Planner[Option[String]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Check if the token is valid
      _ <- IO(logger.info(s"校验token是否有效: ${token}"))
      userIDOpt <- GetUIDByTokenMessage(token).send
      _ <- IO(logger.info(s"获取的userID为: ${userIDOpt.getOrElse("None")}"))
      _ <- IO.whenA(userIDOpt.isEmpty) {
        val errorLog = "无效的Token"
        IO(logger.info(errorLog)) >> IO.pure(Some("Invalid Token"))
      }.flatMap(result => IO(result.toOption.fold(())(IO.raiseError(new RuntimeException(result.get)))))

      // Step 2: Check if the danmaku exists
      _ <- IO(logger.info(s"校验弹幕是否存在: ${danmakuID}"))
      danmakuOpt <- QueryDanmakuByIDMessage(danmakuID).send
      _ <- IO(logger.info(s"查询到的弹幕信息: ${danmakuOpt.map(_.content).getOrElse("None")}"))
      _ <- IO.whenA(danmakuOpt.isEmpty) {
        val errorLog = "弹幕不存在或已被删除"
        IO(logger.info(errorLog)) >> IO.pure(Some("Danmaku Not Found or Deleted"))
      }.flatMap(result => IO(result.toOption.fold(())(IO.raiseError(new RuntimeException(result.get)))))

      // Step 3: Check for duplicate reports
      reporterID = userIDOpt.get
      _ <- IO(logger.info(s"检查重复举报，danmakuID: ${danmakuID}, reporterID: ${reporterID}"))
      duplicateReportExists <- checkDuplicateReport(reporterID, danmakuID)
      _ <- IO(logger.info(s"是否存在重复的Pending状态举报: ${duplicateReportExists}"))
      _ <- IO.whenA(duplicateReportExists) {
        val errorLog = "发现重复举报"
        IO(logger.info(errorLog)) >> IO.pure(Some("Duplicate Pending Report Found"))
      }.flatMap(result => IO(result.toOption.fold(())(IO.raiseError(new RuntimeException(result.get)))))

      // Step 4: Insert the report record
      _ <- IO(logger.info(s"插入举报记录到数据库。举报人ID: ${reporterID}, 弹幕ID: ${danmakuID}, 原因: ${reason}"))
      reportInsertResult <- insertReport(reporterID, danmakuID, reason)

      // Step 5: Notify the user (ignore failure)
      _ <- IO(logger.info(s"发送举报成功的通知给用户ID: ${reporterID}"))
      _ <- sendNotificationToUser(token, reporterID).handleErrorWith { ex =>
        IO(logger.error(s"发送通知失败: ${ex.getMessage}"))
      }

      // Step 6: Return the result
      result = if (reportInsertResult) None else Some("Failed to save report record")
    } yield result
  }

  private def checkDuplicateReport(reporterID: Int, danmakuID: Int)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
SELECT COUNT(*)
FROM ${schemaName}.report_danmaku_table
WHERE reporter_id = ? AND danmaku_id = ? AND status = 'Pending'
       """.stripMargin
    for {
      count <- readDBInt(
        sql,
        List(
          SqlParameter("Int", reporterID.toString),
          SqlParameter("Int", danmakuID.toString)
        )
      )
    } yield count > 0
  }

  private def insertReport(reporterID: Int, danmakuID: Int, reason: String)(using PlanContext): IO[Boolean] = {
    val currentTime = DateTime.now()
    val sql =
      s"""
INSERT INTO ${schemaName}.report_danmaku_table
(danmaku_id, reporter_id, reason, status, timestamp)
VALUES (?, ?, ?, 'Pending', ?)
       """.stripMargin
    writeDB(
      sql,
      List(
        SqlParameter("Int", danmakuID.toString),
        SqlParameter("Int", reporterID.toString),
        SqlParameter("String", reason),
        SqlParameter("DateTime", currentTime.getMillis.toString)
      )
    ).map(_ == "Operation(s) done successfully")
  }

  private def sendNotificationToUser(token: String, reporterID: Int)(using PlanContext): IO[Unit] = {
    val messageContent = "举报已成功提交，感谢您的反馈。"
    sendNotification(token, reporterID, messageContent).attempt.map {
      case Left(exception) =>
        logger.error(s"发送通知失败: ${exception.getMessage}")
      case Right(_) =>
        logger.info(s"通知已成功发送给用户ID: ${reporterID}")
    }
  }
}