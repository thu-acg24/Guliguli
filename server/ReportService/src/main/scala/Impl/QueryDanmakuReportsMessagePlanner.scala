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
import Objects.ReportService.ReportDanmaku
import Objects.ReportService.ReportStatus
import Objects.UserService.UserRole
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryDanmakuReportsMessagePlanner(
                                              token: String,
                                              override val planContext: PlanContext
                                            ) extends Planner[Option[List[ReportDanmaku]]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[List[ReportDanmaku]]] = {
    for {
      // Step 1: Validate user token and permissions
      _ <- IO(logger.info("[Step 1] 开始校验用户Token的有效性及用户权限"))
      maybeUid <- getUserIDFromToken()
      permissionValid <- maybeUid match {
        case Some(uid) => checkUserPermissions(uid)
        case None =>
          IO(logger.info("[Step 1.1] 用户Token无效，直接返回None")) *> IO.pure(false)
      }
      reports <- if (!permissionValid) {
        IO(logger.info("[Step 1.2] 用户不具备审核员权限，返回None")) *> IO.pure(None)
      } else {
        // Step 2: Query danmaku report records
        IO(logger.info("[Step 2] 开始查询弹幕举报记录")) *>
          fetchDanmakuReports().map(Some(_))
      }
    } yield reports
  }

  private def getUserIDFromToken()(using PlanContext): IO[Option[Int]] = {
    for {
      _ <- IO(logger.info("[Step 1.3] 调用 GetUIDByTokenMessage 验证 Token 合法性"))
      maybeUid <- GetUIDByTokenMessage(token).send
      _ <- maybeUid match {
        case Some(uid) => IO(logger.info(s"[Step 1.4] Token合法，关联用户ID为: ${uid}"))
        case None => IO(logger.info("[Step 1.5] Token无效"))
      }
    } yield maybeUid
  }

  private def checkUserPermissions(userID: Int)(using PlanContext): IO[Boolean] = {
    for {
      _ <- IO(logger.info(s"[Step 1.6] 调用 QueryUserRoleMessage 检查用户ID [${userID}] 的权限"))
      maybeRole <- QueryUserRoleMessage(token).send
      permissionValid = maybeRole match {
        case Some(UserRole.Auditor) =>
          logger.info(s"[Step 1.7] 用户ID [${userID}] 权限校验通过，角色为 'Auditor'")
          true
        case Some(role) =>
          logger.info(s"[Step 1.8] 用户ID [${userID}] 角色不是 'Auditor', 而是 [${role}]")
          false
        case None =>
          logger.info(s"[Step 1.9] 用户ID [${userID}] 权限信息获取失败")
          false
      }
    } yield permissionValid
  }

  private def fetchDanmakuReports()(using PlanContext): IO[List[ReportDanmaku]] = {
    val sql =
      s"""
         |SELECT report_id, danmaku_id, reporter_id, reason, status, timestamp
         |FROM ${schemaName}.report_danmaku_table
         |WHERE status = 'Pending'
         |ORDER BY timestamp DESC;
         |""".stripMargin

    IO(logger.info(s"[Step 2.1] 查询弹幕举报记录的SQL：${sql}")) >>
      readDBRows(sql, List.empty).map { resultRows =>
        resultRows.map(parseDanmakuReportRecord)
      }
  }

  private def parseDanmakuReportRecord(json: Json): ReportDanmaku = {
    IO(logger.info(s"[Step 2.2] 开始解析弹幕举报记录: ${json.noSpaces}")).unsafeRunSync()
    ReportDanmaku(
      reportID = decodeField[Int](json, "report_id"),
      danmakuID = decodeField[Int](json, "danmaku_id"),
      reporterID = decodeField[Int](json, "reporter_id"),
      reason = decodeField[String](json, "reason"),
      status = ReportStatus.fromString(decodeField[String](json, "status"))
    )
  }
}