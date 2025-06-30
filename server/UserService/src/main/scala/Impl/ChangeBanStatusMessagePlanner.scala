package Impl

import APIs.UserService.QueryUserRoleMessage
import Objects.UserService.UserRole
import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import cats.implicits.*
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI.*
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}

case class ChangeBanStatusMessagePlanner(
                                          token: String,
                                          userID: Int,
                                          isBan: Boolean,
                                          override val planContext: PlanContext
                                        ) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    val operations = List {
      validateUserRole() // Step 1: 校验用户角色权限
      checkUserExists(userID) // Step 2: 检查目标用户是否存在
      updateBanStatus() // Step 3: 根据检查结果，更新封禁状态或返回错误
    }
    IO(logger.info(s"[ChangeBanStatus] 开始处理封禁状态修改请求")) >>
    operations.foldLeft(IO.pure(None: Option[String])) { (acc, op) =>
      acc.flatMap {
        case None => op // 如果之前没有错误，执行下一个操作
        case result => IO.pure(result) // 如果已有错误，直接返回
      }
    }
  }

  def checkUserExists(userID: Int)(using PlanContext): IO[Option[String]] = {
    val sql =
      s"""
    SELECT 1
    FROM ${schemaName}.user_table
    WHERE user_id = ?
           """.stripMargin

    readDBJsonOptional(sql, List(SqlParameter("Int", userID.toString)))
      .flatMap {
        case Some(_) => IO.pure(None) // 查询到目标用户
        case None =>
          IO(logger.info(s"[ChangeBanStatus] 未在数据库中找到目标用户(userID=${userID})")) >>
            IO.pure(Some("目标用户不存在"))
      }
      .handleErrorWith { ex =>
        IO(logger.error(s"[ChangeBanStatus] 查询目标用户(userID=${userID})时发生错误: ${ex.getMessage}")) >>
          IO.pure(Some("查询目标用户时发生错误"))
      }
  }

  private def validateUserRole()(using PlanContext): IO[Option[String]] = {
    QueryUserRoleMessage(token).send.flatMap { userRoleOpt =>
      userRoleOpt match {
        case Some(UserRole.Auditor) | Some(UserRole.Admin) =>
          IO.pure(None)
        case Some(role) =>
          IO.pure(Some(s"错误：角色'${role.toString}'无权访问"))
        case None =>
          IO.pure(Some(s"token不合法: $token"))
      }
    }
  }


  private def updateBanStatus()(using PlanContext): IO[Option[String]] = {
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
    ).map(_ => {
      logger.info(s"[ChangeBanStatus] 成功更新用户(userID=${userID})封禁状态为: ${isBan}")
      None
    })
    .handleErrorWith { ex =>
      IO(logger.error(s"[ChangeBanStatus] 更新用户(userID=${userID})封禁状态失败: ${ex.getMessage}")) >>
        IO(Some("Failed to change ban status"))
    }
  }
}