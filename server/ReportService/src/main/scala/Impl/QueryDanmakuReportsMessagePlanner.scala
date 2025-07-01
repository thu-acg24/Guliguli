package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.ReportService.ReportDanmaku
import Objects.ReportService.ReportStatus
import Objects.UserService.UserRole
import Utils.ValidateProcess.validateTokenAndRole
import cats.effect.IO
import cats.implicits.*
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryDanmakuReportsMessagePlanner(
                                              token: String,
                                              override val planContext: PlanContext
                                            ) extends Planner[List[ReportDanmaku]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[ReportDanmaku]] = {
    for {
      // Step 1: Verify Token and Auditor Role
      _ <- validateTokenAndRole(token)
      result <- queryPendingReports
    } yield result
  }

  private def queryPendingReports(using PlanContext): IO[List[ReportDanmaku]] = {
    val query =
      s"""
         |SELECT report_id, danmaku_id, reporter_id, reason, status, timestamp
         |FROM ${schemaName}.report_danmaku_table
         |WHERE status = ?
         |ORDER BY timestamp ASC;
         """.stripMargin

    val params = List(SqlParameter("String", ReportStatus.Pending.toString))
    logger.info(s"查询待处理举报评论记录的SQL：${query}，参数：${params}")

    readDBRows(query, params).map(_.map(parseReportDanmaku))
  }

  private def parseReportDanmaku(json: Json): ReportDanmaku = {
    val reportID = decodeField[Int](json, "report_id")
    val danmakuID = decodeField[Int](json, "danmaku_id")
    val reporterID = decodeField[Int](json, "reporter_id")
    val reason = decodeField[String](json, "reason")
    val status = ReportStatus.fromString(decodeField[String](json, "status"))
    val timestamp = decodeField[DateTime](json, "timestamp")
    ReportDanmaku(reportID, danmakuID, reporterID, reason, status, timestamp)
  }
}