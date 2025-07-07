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
) extends Planner[UserRole] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[UserRole] = {
    for {
      // Step 1: Validate token and extract userID
      _ <- IO(logger.info(s"[Step 1] 开始处理用户角色查询，根据 token=$token"))
      userID <- validateToken(token)

      // Step 2: Query userRole from UserTable based on userID
      userRole <- queryUserRole(userID)
    } yield userRole
  }

  private def queryUserRole(userID: Int)(using PlanContext): IO[UserRole] = {
    // Step 2.1: Prepare SQL and parameters
    val sql = s"SELECT user_role FROM $schemaName.user_table WHERE user_id = ?"
    val params = List(SqlParameter("Int", userID.toString))

    for {
      _ <- IO(logger.info(s"[Step 2.1] 准备查询角色，查询SQL: $sql, 参数: $params"))
      // Step 2.2: Execute DB query
      userRecord <- readDBJson(sql, params)

      // Step 2.3: Process result
      userRole <- IO {
        val userRoleStr = decodeField[String](userRecord, "user_role")
        logger.info(s"[Step 2.3] 查询完成，用户角色为: $userRecord")
        UserRole.fromString(userRoleStr) // Throws exception for invalid string
      }
    } yield userRole
  }
}