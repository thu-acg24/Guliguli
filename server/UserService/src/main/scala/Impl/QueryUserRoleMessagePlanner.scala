package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Utils.AuthProcess.validateToken
import cats.effect.IO
import cats.implicits.*
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryUserRoleMessagePlanner(
    token: String,
    override val planContext: PlanContext
) extends Planner[Option[UserRole]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[UserRole]] = {
    for {
      // Step 1: Validate token and extract userID
      _ <- IO(logger.info(s"[Step 1] 开始处理用户角色查询，根据 token=${token}"))
      userIDOpt <- validateToken(token)

      // Step 2: Query userRole from UserTable based on userID
      userRoleOpt <- queryUserRole(userIDOpt)

      // Final log summary
      _ <- IO(logger.info(s"结束处理用户角色查询，结果为: ${userRoleOpt.fold("无记录")(r => r.toString)}"))
    } yield userRoleOpt
  }

  private def queryUserRole(userIDOpt: Option[Int])(using PlanContext): IO[Option[UserRole]] = {
    userIDOpt match {
      case Some(userID) =>
        // Step 2.1: Prepare SQL and parameters
        val sql = s"SELECT user_role FROM ${schemaName}.user_table WHERE user_id = ?"
        val params = List(SqlParameter("Int", userID.toString))

        for {
          _ <- IO(logger.info(s"[Step 2.1] 准备查询角色，查询SQL: ${sql}, 参数: ${params}"))
          // Step 2.2: Execute DB query
          userRecordOpt <- readDBJsonOptional(sql, params)

          // Step 2.3: Process result
          userRoleOpt <- IO {
            userRecordOpt.map { record =>
              val userRoleStr = decodeField[String](record, "user_role")
              UserRole.fromString(userRoleStr) // Throws exception for invalid string
            }
          }

          _ <- IO(logger.info(s"[Step 2.3] 查询完成，用户角色为: ${userRoleOpt.fold("无记录")(r => r.toString)}"))
        } yield userRoleOpt

      case None =>
        IO {
          logger.info("[Step 2.4] 用户ID未找到，返回None")
          None
        }
    }
  }
}