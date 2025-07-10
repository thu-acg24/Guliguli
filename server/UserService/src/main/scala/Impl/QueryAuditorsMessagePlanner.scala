package Impl


import APIs.UserService.QueryUserRoleMessage
import Common.APIException.InvalidInputException
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.ParameterList
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.minioClient
import Objects.UserService.UserInfo
import Objects.UserService.UserRole
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.http.Method
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

case class QueryAuditorsMessagePlanner(
  token: String, 
  override val planContext: PlanContext
) extends Planner[List[UserInfo]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[UserInfo]] = {
    for {
      // Step 1: 验证用户角色权限
      _ <- IO(logger.info(s"[Step 1] 开始校验用户Token是否具有管理员权限"))
      userRole <- validateUserRole(token)

      result <- userRole match {
        case role if role != UserRole.Admin =>
          IO(logger.warn(s"[Step 1.2] 用户角色无权限，当前角色：$role")) *>
            IO.raiseError(InvalidInputException("不具有管理员权限")) // 返回None表示非Admin无权限
        case _ =>
          // Step 2: 查询审核员列表
          IO(logger.info(s"[Step 2] 用户角色校验通过，开始查询所有审核员")) *>
            fetchAuditors()
      }
    } yield result
  }

  /**
   * 验证当前Token对应的用户角色
   * @param token 用户登录后的Token
   * @return Option[UserRole] - 如果Token无效，返回None；否则返回对应的用户角色
   */
  private def validateUserRole(token: String)(using PlanContext): IO[UserRole] = {
    IO(logger.info(s"调用权限校验API: QueryUserRoleMessage(token=$token)")) *>
      QueryUserRoleMessage(token).send
  }

  /**
   * 从数据库查询所有`user_role`为`Auditor`的用户信息
   * @return List[UserInfo] - 返回查询结果列表，满足条件的用户的所有信息
   */
  private def fetchAuditors()(using PlanContext): IO[List[UserInfo]] = {
    val sql =
      s"""
         SELECT user_id, username, avatar_path, bio, is_banned
         FROM $schemaName.user_table
         WHERE user_role = ?
       """
    val params = List(SqlParameter("String", UserRole.Auditor.toString))

    for {
      _ <- IO(logger.info(s"[Step 2.1] SQL查询命令生成完成，准备执行。SQL: $sql, 参数: $params"))
      rows <- readDBRows(sql, params)

      // 将数据库行转为UserInfo对象
      auditors <- rows.traverse { row =>
        for {
          avatarUrl <- IO.blocking {
            minioClient.getPresignedObjectUrl(
              io.minio.GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket("avatar")
                .`object`(decodeField[String](row, "avatar_path"))
                .expiry(1, TimeUnit.DAYS)
                .build()
            )
          }
        }
        yield UserInfo(
          userID = decodeField[Int](row, "user_id"),
          username = decodeField[String](row, "username"),
          avatarPath = avatarUrl,
          bio = decodeField[String](row, "bio"),
          isBanned = decodeField[Boolean](row, "is_banned")
        )
      }

      _ <- if (auditors.isEmpty)
             IO(logger.info(s"[Step 2.3] 未找到任何审核员，返回空列表"))
           else
             IO(logger.info(s"[Step 2.3] 找到${auditors.length}位审核员"))

    } yield auditors
  }
}