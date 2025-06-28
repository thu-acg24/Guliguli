package Impl


import APIs.UserService.QueryUserInfoMessage
import Objects.UserService.UserRole
import APIs.UserService.QueryUserRoleMessage
import Objects.UserService.UserInfo
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import cats.implicits.*
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
import Objects.UserService.UserInfo
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ChangeUserRoleMessagePlanner(
  token: String,
  userID: Int,
  newRole: UserRole,
  override val planContext: PlanContext
) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: 确认操作用户具有管理员权限
      _ <- IO(logger.info(s"[Step 1] 验证Token=${token}对应用户的权限"))
      maybeOperatorRole <- getOperatorRole()
      _ <- IO.whenA(maybeOperatorRole.isEmpty || maybeOperatorRole.get != UserRole.Admin) {
        IO(logger.error(s"Token=${token}的用户无操作权限, 实际角色=${maybeOperatorRole.getOrElse("None")}")) >>
          IO.raiseError(new IllegalArgumentException("Unauthorized Access"))
      }

      // Step 2: 确认目标用户是否存在
      _ <- IO(logger.info(s"[Step 2] 验证用户ID=${userID}是否存在"))
      maybeTargetUser <- getTargetUserInfo()
      _ <- IO.whenA(maybeTargetUser.isEmpty) {
        IO(logger.error(s"未找到用户ID=${userID}的信息")) >>
          IO.raiseError(new IllegalArgumentException("Target user not found"))
      }

      // Step 3: 验证角色更改的合法性
      _ <- IO(logger.info(s"[Step 3] 开始验证新角色=${newRole}的合法性"))
      _ <- validateNewRoleValidity()

      // Step 4: 更新用户角色信息
      _ <- IO(logger.info(s"[Step 4] 更新用户ID=${userID}的角色为${newRole}"))
      updateResult <- updateUserRoleInDB()
      _ <- IO.whenA(updateResult == 0) {
        IO(logger.error(s"[Step 4] 用户角色更新失败，用户ID=${userID}，新角色=${newRole}")) >>
          IO.raiseError(new IllegalArgumentException("Failed to update user role"))
      }

      _ <- IO(logger.info(s"[Step 5] 用户角色更新成功！用户ID=${userID}, 新角色=${newRole}"))
    } yield None
  }

  // Step 1 Helper: 获取操作者角色
  private def getOperatorRole()(using PlanContext): IO[Option[UserRole]] = {
    QueryUserRoleMessage(token).send.flatMap {
      case Some(role) =>
        IO(logger.info(s"Token=${token}对应用户角色=${role}")) >> IO.pure(Some(role))
      case None =>
        IO(logger.warn(s"无法通过Token=${token}获取对应用户角色")) >> IO.pure(None)
    }
  }

  // Step 2 Helper: 获取目标用户信息
  private def getTargetUserInfo()(using PlanContext): IO[Option[UserInfo]] = {
    QueryUserInfoMessage(userID).send.flatMap {
      case Some(user) =>
        IO(logger.info(s"目标用户存在: 用户名=${user.username}, 用户ID=${userID}")) >> IO.pure(Some(user))
      case None =>
        IO(logger.error(s"目标用户不存在, 用户ID=${userID}")) >> IO.pure(None)
    }
  }

  // Step 3 Helper: 验证 newRole 的合法性
  private def validateNewRoleValidity()(using PlanContext): IO[Unit] = IO {
    if (newRole == UserRole.Admin) {
      val errorMessage = "Cannot assign Admin role to other users"
      logger.error(errorMessage)
      throw new IllegalArgumentException(errorMessage)
    }
    UserRole.fromString(newRole.toString) // 验证枚举值兼容性，若非法会抛出异常
    logger.info(s"角色 newRole=${newRole} 验证通过")
  }

  // Step 4 Helper: 更新数据库中的用户角色
  private def updateUserRoleInDB()(using PlanContext): IO[Int] = {
    val updateSQL =
      s"""
         |UPDATE ${schemaName}.user_table
         |SET user_role = ?, updated_at = ?
         |WHERE user_id = ?;
         |""".stripMargin

    val params = List(
      SqlParameter("String", newRole.toString),
      SqlParameter("DateTime", DateTime.now().getMillis.toString),
      SqlParameter("Int", userID.toString)
    )

    readDBInt(updateSQL, params)
  }
}