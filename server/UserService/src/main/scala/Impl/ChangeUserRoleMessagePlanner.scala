package Impl


import APIs.UserService.QueryUserRoleMessage
import Common.APIException.InvalidInputException
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ChangeUserRoleMessagePlanner(
  token: String,
  userID: Int,
  newRole: UserRole,
  override val planContext: PlanContext
) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"[ChangeUserRole] 开始处理修改用户权限请求"))
      _ <- getOperatorRole()
      _ <- checkUserExists(userID)
      _ <- validateNewRoleValidity()
      _ <- updateUserRoleInDB()
    } yield()
  }

  // Step 1 Helper: 获取操作者角色
  private def getOperatorRole()(using PlanContext): IO[Unit] = {
    QueryUserRoleMessage(token).send.flatMap {
      case UserRole.Admin =>
        IO(logger.info(s"Token=${token}对应用户角色=${UserRole.Admin}")).void
      case role =>
        IO(logger.info(s"Token=${token}对应用户角色=$role")) *>
        IO.raiseError(new InvalidInputException(s"操作人($role)权限不足"))
    }
  }

  // Step 2 Helper: 查看目标用户是否存在
  def checkUserExists(userID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
  SELECT 1
  FROM $schemaName.user_table
  WHERE user_id = ?
         """.stripMargin

    readDBJsonOptional(sql, List(SqlParameter("Int", userID.toString)))
      .flatMap {
        case Some(_) => IO.unit // 查询到目标用户
        case None =>
          IO(logger.info(s"[ChangeUserRoleStatus] 未在数据库中找到目标用户(userID=$userID)")) *>
          IO.raiseError(new InvalidInputException("未在数据库中找到目标用户"))
      }
  }

  // Step 3 Helper: 验证 newRole 的合法性
  private def validateNewRoleValidity()(using PlanContext): IO[Unit] = {
    if (newRole == UserRole.Admin) {
      IO(logger.error("无法将权限修改为管理员")) *>
      IO.raiseError(new InvalidInputException("无法将权限修改为管理员"))
    } else {
      IO(UserRole.fromString(newRole.toString))
        .attempt // 将结果转换为Either[Throwable, UserRole]
        .flatMap {
          case Right(_) =>
            IO(logger.info(s"角色验证通过: $newRole")).void
          case Left(ex) =>
            IO(logger.error(s"无效角色: ${ex.getMessage}")) *>
            IO.raiseError(new InvalidInputException("给定权限不合法"))
        }
    }
  }

  // Step 4 Helper: 更新数据库中的用户角色
  private def updateUserRoleInDB()(using PlanContext): IO[Unit] = {
    val updateSQL =
      s"""
         |UPDATE $schemaName.user_table
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
        if (updatedRows <= 0)
          IO(logger.info(s"未更新任何记录，可能用户不存在(userID=$userID)")) *>
            IO.raiseError(new InvalidInputException(s"未更新任何记录，可能用户不存在"))
      }
  }
}