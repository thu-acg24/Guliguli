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
import Objects.ReportService.ReportStatus
import Objects.ReportService.ReportVideo
import Objects.UserService.UserRole
import Utils.ValidateProcess.validateTokenAndRole
import cats.effect.IO
import cats.implicits.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryVideoReportsMessagePlanner(
                                            token: String,
                                            override val planContext: PlanContext
                                          ) extends Planner[List[ReportVideo]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[ReportVideo]] = {
    for {
      // Step 1: Verify Token and Auditor Role
      _ <- validateTokenAndRole(token)
      result <- queryPendingReports
    } yield result
  }

  private def queryPendingReports(using PlanContext): IO[List[ReportVideo]] = {
    val query =
      s"""
         |SELECT report_id, video_id, reporter_id, reason, status, timestamp
         |FROM $schemaName.report_video_table
         |WHERE status = ?
         |ORDER BY timestamp ASC;
         """.stripMargin

    val params = List(SqlParameter("String", ReportStatus.Pending.toString))
    logger.info(s"查询待处理举报评论记录的SQL：$query，参数：$params")

    readDBRows(query, params).map(_.map(parseReportVideo))
  }

  private def parseReportVideo(json: Json): ReportVideo = {
    val reportID = decodeField[Int](json, "report_id")
    val videoID = decodeField[Int](json, "video_id")
    val reporterID = decodeField[Int](json, "reporter_id")
    val reason = decodeField[String](json, "reason")
    val status = ReportStatus.fromString(decodeField[String](json, "status"))
    val timestamp = decodeField[DateTime](json, "timestamp")
    ReportVideo(reportID, videoID, reporterID, reason, status, timestamp)
  }
}