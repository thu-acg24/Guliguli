package Impl

import APIs.UserService.QueryUserRoleMessage
import Objects.UserService.UserRole
import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import cats.effect.IO
import cats.implicits.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*

case class ChangeBanStatusMessagePlanner(
                                          token: String,
                                          userID: Int,
                                          isBan: Boolean,
                                          override val planContext: PlanContext
                                        ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"[ChangeBanStatus] 开始检查管理员权限"))
      _ <- validateUserRole()
      _ <- IO(logger.info(s"[ChangeBanStatus] 开始检查用户存在性"))
      _ <- checkUserExists(userID)
      _ <- IO(logger.info(s"[ChangeBanStatus] 开始处理封禁状态修改请求"))
      _ <- updateBanStatus()
    } yield ()
  }

  def checkUserExists(userID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
    SELECT 1
    FROM ${schemaName}.user_table
    WHERE user_id = ?
           """.stripMargin

    readDBJsonOptional(sql, List(SqlParameter("Int", userID.toString)))
      .flatMap {
        case Some(_) => IO.unit // 查询到目标用户
        case None =>
            IO(logger.info(s"[ChangeBanStatus] 未在数据库中找到目标用户(userID=${userID})")) *>
            IO.raiseError(new RuntimeException(s"未在数据库中找到目标用户"))
      }.handleErrorWith { ex =>
          IO(logger.error(s"[ChangeBanStatus] 获取用户(userID=${userID})信息时失败: ${ex.getMessage}")) *>
          IO.raiseError(new RuntimeException(s"获取用户信息时失败: ${ex.getMessage}"))
      }
  }

  private def validateUserRole()(using PlanContext): IO[Unit] = {
    QueryUserRoleMessage(token).send.flatMap { userRoleOpt =>
      userRoleOpt match {
        case Some(UserRole.Auditor) | Some(UserRole.Admin) => IO.unit
        case Some(role) =>
          IO(logger.info(s"[ChangeBanStatus] 错误：角色'${role.toString}'无权访问")) *>
          IO.raiseError(new RuntimeException(s"错误：角色'${role.toString}'无权访问"))
      }
    }
  }


  private def updateBanStatus()(using PlanContext): IO[Unit] = {
    val sql =
      s"""
UPDATE ${schemaName}.user_table
SET is_banned = ?, updated_at = ?
WHERE user_id = ?
       """.stripMargin

    val currentTimeMillis = DateTime.now.getMillis.toString
    writeDB(
      sql,
      List(
        SqlParameter("Boolean", isBan.toString),
        SqlParameter("DateTime", currentTimeMillis),
        SqlParameter("Int", userID.toString)
      )
    ).map(_ =>
      logger.info(s"[ChangeBanStatus] 成功更新用户(userID=${userID})封禁状态为: ${isBan}")
    )
    .handleErrorWith { ex =>
      IO(logger.error(s"[ChangeBanStatus] 更新用户(userID=${userID})封禁状态失败: ${ex.getMessage}")) *>
      IO.raiseError(new RuntimeException(s"更新用户封禁状态失败: ${ex.getMessage}"))
    }
  }
}