package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.ReportService.ReportComment
import Objects.ReportService.ReportStatus
import Objects.UserService.UserRole
import Utils.ValidateProcess.validateTokenAndRole
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryCommentReportsMessagePlanner(
                                              token: String,
                                              override val planContext: PlanContext
                                            ) extends Planner[List[ReportComment]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[ReportComment]] = {
    for {
      // Step 1: Verify Token and Auditor Role
      _ <- validateTokenAndRole(token)
      result <- queryPendingReports
    } yield result
  }

  private def queryPendingReports(using PlanContext): IO[List[ReportComment]] = {
    val query =
      s"""
         |SELECT report_id, comment_id, reporter_id, reason, status, timestamp
         |FROM $schemaName.report_comment_table
         |WHERE status = ?
         |ORDER BY timestamp ASC;
         """.stripMargin

    val params = List(SqlParameter("String", ReportStatus.Pending.toString))
    logger.info(s"查询待处理举报评论记录的SQL：$query，参数：$params")

    readDBRows(query, params).map(_.map(parseReportComment))
  }

  private def parseReportComment(json: Json): ReportComment = {
    val reportID = decodeField[Int](json, "report_id")
    val commentID = decodeField[Int](json, "comment_id")
    val reporterID = decodeField[Int](json, "reporter_id")
    val reason = decodeField[String](json, "reason")
    val status = ReportStatus.fromString(decodeField[String](json, "status"))
    val timestamp = decodeField[DateTime](json, "timestamp")
    ReportComment(reportID, commentID, reporterID, reason, status, timestamp)
  }
}