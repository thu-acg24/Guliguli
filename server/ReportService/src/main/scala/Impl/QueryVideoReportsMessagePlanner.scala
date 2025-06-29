package Impl


import Objects.ReportService.ReportVideo
import Objects.UserService.UserRole
import APIs.UserService.QueryUserRoleMessage
import Objects.ReportService.ReportStatus
import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
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

case class QueryVideoReportsMessagePlanner(
                                            token: String,
                                            override val planContext: PlanContext
                                          ) extends Planner[Option[List[ReportVideo]]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[List[ReportVideo]]] = {
    for {
      // Step 1: 校验用户Token及获取审核员权限
      userID <- validateToken()
      _ <- IO(logger.info(s"获取到的用户ID为：${userID.getOrElse("无效Token")}"))
      isAuditor <- checkAuditorRole(userID)
      _ <- IO(logger.info(s"用户是否具备审核员权限：$isAuditor"))

      // Step 2: 查询待处理的举报记录（仅审核员能进入这一步）
      reports <- if (isAuditor) getPendingReports() else IO(Some(List.empty[ReportVideo]))
      _ <- IO(logger.info(s"返回的举报记录条数为：${reports.map(_.size).getOrElse(0)}"))
    } yield reports
  }

  // 子步骤1.1 校验Token有效性并获取用户ID
  private def validateToken()(using PlanContext): IO[Option[Int]] = {
    IO(logger.info("调用 GetUIDByTokenMessage 接口校验Token及获取用户ID")) >>
    GetUIDByTokenMessage(token).send
  }

  // 子步骤1.2 检查用户是否具备审核员权限
  private def checkAuditorRole(userIDOption: Option[Int])(using PlanContext): IO[Boolean] = {
    userIDOption match {
      case Some(userID) =>
        IO(logger.info(s"调用 QueryUserRoleMessage 接口验证用户ID[$userID]是否具备审核员权限")) >>
        QueryUserRoleMessage(token).send.map {
          case Some(role) if role == UserRole.Auditor =>
            logger.info("用户角色验证为审核员，权限校验通过")
            true
          case _ =>
            logger.info("用户角色验证非审核员，权限校验失败")
            false
        }
      case None =>
        IO(logger.info("Token无效，无法获取用户ID，权限校验失败")) >>
        IO.pure(false)
    }
  }

  // 步骤2：查询所有待处理举报记录ReportVideoTable (status为Pending)
  private def getPendingReports()(using PlanContext): IO[Option[List[ReportVideo]]] = {
    IO(logger.info("开始创建查询举报记录的数据库SQL指令")) >>
    IO {
      val sql =
        s"""
           SELECT report_id, video_id, reporter_id, reason, status, timestamp
           FROM ${schemaName}.report_video_table
           WHERE status = ?
           ORDER BY timestamp DESC;
         """.stripMargin
      val parameters = List(SqlParameter("String", ReportStatus.Pending.toString))
      logger.info(s"数据库指令为：$sql，参数为：$parameters")
      (sql, parameters)
    }.flatMap { case (sql, parameters) =>
      IO(logger.info("开始执行查询举报记录")) >>
      readDBRows(sql, parameters).map { rows =>
        val reports = rows.map { json =>
          ReportVideo(
            reportID = decodeField[Int](json, "report_id"),
            videoID = decodeField[Int](json, "video_id"),
            reporterID = decodeField[Int](json, "reporter_id"),
            reason = decodeField[String](json, "reason"),
            status = ReportStatus.fromString(decodeField[String](json, "status")),
            timestamp = new DateTime(decodeField[Long](json, "timestamp"))
          )
        }
        if (reports.isEmpty) {
          logger.info("没有查询到任何待处理举报记录，返回空列表")
          Some(List.empty[ReportVideo])
        } else {
          logger.info(s"查询到${reports.size}条举报记录")
          Some(reports)
        }
      }
    }
  }
}