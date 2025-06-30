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
    val operations = List {
      getOperatorRole()
      checkUserExists(userID)
      validateNewRoleValidity()
      updateUserRoleInDB()
    }
    IO(logger.info(s"[ChangeUserRole] 开始处理修改用户权限请求")) >>
    operations.foldLeft(IO.pure(None: Option[String])) { (acc, op) =>
      acc.flatMap {
        case None => op // 如果之前没有错误，执行下一个操作
        case result => IO.pure(result) // 如果已有错误，直接返回
      }
    }
  }

  // Step 1 Helper: 获取操作者角色
  private def getOperatorRole()(using PlanContext): IO[Option[String]] = {
    QueryUserRoleMessage(token).send.flatMap {
      case Some(UserRole.Admin) =>
        IO(logger.info(s"Token=${token}对应用户角色=${UserRole.Admin}")) >> IO.pure(None)
      case Some(role) =>
        IO(logger.info(s"Token=${token}对应用户角色=${role}")) >> IO.pure(Some("操作人权限不足"))
      case None =>
        IO(logger.warn(s"无法通过Token=${token}获取对应用户角色")) >> IO.pure(Some("无法获取操作人权限"))
    }
  }

  // Step 2 Helper: 查看目标用户是否存在
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
          IO(logger.info(s"[ChangeUserRoleStatus] 未在数据库中找到目标用户(userID=${userID})")) >>
            IO.pure(Some("目标用户不存在"))
      }
      .handleErrorWith { ex =>
        IO(logger.error(s"[ChangeUserRoleStatus] 查询目标用户(userID=${userID})时发生错误: ${ex.getMessage}")) >>
          IO.pure(Some("查询目标用户时发生错误"))
      }
  }

  // Step 3 Helper: 验证 newRole 的合法性
  private def validateNewRoleValidity()(using PlanContext): IO[Option[String]] = {
    if (newRole == UserRole.Admin) {
      IO(logger.error("Cannot assign Admin role")) >>
        IO.pure(Some("无法将权限修改为管理员"))
    } else {
      IO(UserRole.fromString(newRole.toString))
        .attempt // 将结果转换为Either[Throwable, UserRole]
        .flatMap {
          case Right(_) =>
            IO(logger.info(s"角色验证通过: $newRole")) >> IO.pure(None)
          case Left(ex) =>
            IO(logger.error(s"无效角色: ${ex.getMessage}")) >>
              IO.pure(Some("给定权限不合法"))
        }
    }
  }

  // Step 4 Helper: 更新数据库中的用户角色
  private def updateUserRoleInDB()(using PlanContext): IO[Option[String]] = {
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

    // 执行更新操作并处理结果
    readDBInt(updateSQL, params)
      .map { updatedRows =>
        if (updatedRows > 0) None // 更新成功
        else Some(s"未更新任何记录，可能用户不存在(userID=$userID)")
      }
      .handleErrorWith { ex =>
        IO(logger.error(s"更新用户角色失败(userID=$userID): ${ex.getMessage}")) >>
          IO.pure(Some(s"数据库更新失败: ${ex.getMessage}"))
      }
  }
}