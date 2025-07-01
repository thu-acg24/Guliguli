package Impl


import APIs.DanmakuService.QueryDanmakuByIDMessage
import APIs.MessageService.SendMessageMessage
import APIs.UserService.GetUIDByTokenMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.DanmakuService.Danmaku
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ReportDanmakuContentMessagePlanner(
    token: String,
    danmakuID: Int,
    reason: String,
    override val planContext: PlanContext
) extends Planner[Unit] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      // Step 1: 校验 token 是否有效并获取 userID
      _ <- IO(logger.info(s"Validating token: ${token}"))
      userID <- GetUIDByTokenMessage(token).send
      // Step 2: 检测弹幕是否存在
      _ <- QueryDanmakuByIDMessage(danmakuID).send
      // Step 3: 检查重复举报
      _ <- IO(logger.info(s"Checking for duplicate pending reports for danmakuID: ${danmakuID} and userID: ${userID}"))
      alreadyExists <- checkDuplicateReport(danmakuID, userID)
      _ <- if alreadyExists then IO.raiseError(RuntimeException("已经举报过该弹幕")) else IO.unit
      // Step 4: 插入举报记录
      _ <- IO(logger.info(s"Inserting new report for danmakuID: ${danmakuID}, userID: ${userID}, reason: ${reason}"))
      _ <- insertReportRecord(userID, danmakuID, reason)
    } yield ()
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

  private def insertReportRecord(userID: Int, danmakuID: Int, reason: String)(using PlanContext): IO[String] = {
    val sql =
      s"""
         INSERT INTO ${schemaName}.report_danmaku_table
         (danmaku_id, reporter_id, reason, status, timestamp)
         VALUES (?, ?, ?, 'Pending', ?)
       """
    IO(DateTime.now().getMillis.toString).flatMap { timestamp =>
      writeDB(sql, List(
        SqlParameter("Int", danmakuID.toString),
        SqlParameter("Int", userID.toString),
        SqlParameter("String", reason),
        SqlParameter("DateTime", timestamp)
      ))
    }
  }
}