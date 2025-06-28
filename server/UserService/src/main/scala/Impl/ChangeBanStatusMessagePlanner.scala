package Impl


import APIs.UserService.QueryUserRoleMessage
import Objects.UserService.UserRole
import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}
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
    for {
      _ <- IO(logger.info(s"[ChangeBanStatus] 开始处理封禁状态修改请求，验证用户权限"))

      // Step 1: 校验用户角色权限
      userRoleOpt <- validateUserRole()
      _ <- IO(logger.info(s"[ChangeBanStatus] 当前用户角色校验结果: ${userRoleOpt.map(_.toString).getOrElse("None")}"))

      result <- userRoleOpt match {
        case Some(UserRole.Auditor) =>
          for {
            _ <- IO(logger.info(s"[ChangeBanStatus] 当前用户有审核员(Auditor)权限，继续执行目标用户检查"))

            // Step 2: 检查目标用户是否存在
            targetUserExists <- checkUserExists()
            _ <- IO(logger.info(s"[ChangeBanStatus] 目标用户存在性检查结果: ${targetUserExists}"))

            // Step 3: 根据检查结果，更新封禁状态或返回错误
            changeResult <- if (targetUserExists) {
              IO(logger.info(s"[ChangeBanStatus] 准备更新目标用户(userID=${userID})的封禁状态为: ${isBan}")) >>
                updateBanStatus()
            } else {
              IO(logger.warn(s"[ChangeBanStatus] 目标用户(userID=${userID})不存在")) >>
                IO(Some("Target user not found"))
            }
          } yield changeResult

        case _ => 
          // 用户没有足够权限
          IO(logger.warn(s"[ChangeBanStatus] 用户无权限操作 (Unauthorized Access)")) >>
          IO(Some("Unauthorized Access"))
      }
    } yield result
  }

  private def validateUserRole()(using PlanContext): IO[Option[UserRole]] = {
    QueryUserRoleMessage(token).send.map { userRoleOpt =>
      userRoleOpt.collect {
        case role if role == UserRole.Auditor => role
      }
    }
  }

  private def checkUserExists()(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
SELECT 1
FROM ${schemaName}.user_table
WHERE user_id = ?
       """.stripMargin

    readDBJsonOptional(sql, List(SqlParameter("Int", userID.toString)))
      .map {
        case Some(_) => true // 查询到目标用户
        case None =>
          logger.info(s"[ChangeBanStatus] 未在数据库中找到目标用户(userID=${userID})")
          false
      }
      .handleErrorWith { ex =>
        IO(logger.error(s"[ChangeBanStatus] 查询目标用户(userID=${userID})时发生错误: ${ex.getMessage}")) >>
          IO(false)
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