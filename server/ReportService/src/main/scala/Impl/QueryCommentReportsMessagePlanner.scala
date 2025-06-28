package Impl


import Objects.ReportService.ReportComment
import Objects.UserService.UserRole
import APIs.UserService.QueryUserRoleMessage
import Objects.ReportService.ReportStatus
import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
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
import APIs.UserService.getUIDByTokenMessage
import io.circe._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryCommentReportsMessagePlanner(
                                              token: String,
                                              override val planContext: PlanContext
                                            ) extends Planner[Option[List[ReportComment]]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[List[ReportComment]]] = {
    for {
      // Step 1: Verify Token and Auditor Role
      isAuditor <- validateUserTokenAndRole()
      reportsOpt <- if (isAuditor) {
        // Step 2: Query Pending Comment Reports
        queryPendingReports.map(reports => {
          logger.info(s"查询完成，得到的记录数量为：${reports.length}")
          Some(reports)
        })
      } else {
        IO(logger.info("用户Token无效或用户不具备审核员权限，返回None")) *> IO.pure(None)
      }
    } yield reportsOpt
  }

  private def validateUserTokenAndRole()(using PlanContext): IO[Boolean] = {
    for {
      _ <- IO(logger.info("调用getUIDByTokenMessage验证Token合法性"))
      userIDOpt <- getUIDByTokenMessage(token).send
      isAuditor <- userIDOpt match {
        case Some(userID) =>
          IO(logger.info(s"Token合法，对应的用户ID为：${userID}")) >>
          IO(logger.info("调用QueryUserRoleMessage获取用户角色信息")) >>
          QueryUserRoleMessage(token).send.map {
            case Some(UserRole.Auditor) =>
              logger.info("用户角色为Auditor，具备审核员权限")
              true
            case _ =>
              logger.info("用户角色不为Auditor，拒绝操作")
              false
          }
        case None =>
          IO(logger.info("Token无效")) *> IO.pure(false)
      }
    } yield isAuditor
  }

  private def queryPendingReports(using PlanContext): IO[List[ReportComment]] = {
    val query =
      s"""
         |SELECT report_id, comment_id, reporter_id, reason, status, timestamp
         |FROM ${schemaName}.report_comment_table
         |WHERE status = ?
         |ORDER BY timestamp DESC;
         """.stripMargin

    val params = List(SqlParameter("String", ReportStatus.Pending.toString))
    logger.info(s"查询待处理举报评论记录的SQL：${query}，参数：${params}")

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