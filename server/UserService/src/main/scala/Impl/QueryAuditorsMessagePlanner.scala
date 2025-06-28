package Impl


import Objects.UserService.UserRole
import APIs.UserService.QueryUserRoleMessage
import Objects.UserService.UserInfo
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import Common.Object.SqlParameter
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
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
import Common.Object.ParameterList
import io.circe.syntax._
import io.circe._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryAuditorsMessagePlanner(
  token: String, 
  override val planContext: PlanContext
) extends Planner[Option[List[UserInfo]]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[List[UserInfo]]] = {
    for {
      // Step 1: 验证用户角色权限
      _ <- IO(logger.info(s"[Step 1] 开始校验用户Token是否具有管理员权限"))
      userRoleOpt <- validateUserRole(token)

      result <- userRoleOpt match {
        case None =>
          IO(logger.warn(s"[Step 1.1] 用户Token无效，未通过权限校验")) *>
            IO.pure(None) // 返回None表示校验失败

        case Some(role) if role != UserRole.Admin =>
          IO(logger.warn(s"[Step 1.2] 用户角色无权限，当前角色：${role}")) *>
            IO.pure(None) // 返回None表示非Admin无权限

        case Some(_) =>
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
  private def validateUserRole(token: String)(using PlanContext): IO[Option[UserRole]] = {
    IO(logger.info(s"调用权限校验API: QueryUserRoleMessage(token=${token})")) *>
      QueryUserRoleMessage(token).send
  }

  /**
   * 从数据库查询所有`user_role`为`Auditor`的用户信息
   * @return Option[List[UserInfo]] - 返回查询结果列表，满足条件的用户的所有信息
   */
  private def fetchAuditors()(using PlanContext): IO[Option[List[UserInfo]]] = {
    val sql =
      s"""
         SELECT user_id, username, avatar_path, is_banned
         FROM ${schemaName}.user_table
         WHERE user_role = ?
       """
    val params = List(SqlParameter("String", UserRole.Auditor.toString))

    for {
      _ <- IO(logger.info(s"[Step 2.1] SQL查询命令生成完成，准备执行。SQL: ${sql}, 参数: ${params}"))
      rows <- readDBRows(sql, params).handleErrorWith { error =>
        IO(logger.error(s"[Step 2.2] 查询数据库时发生错误: ${error.getMessage}")) *>
          IO.pure(List.empty[Json]) // 返回空列表以避免程序中断
      }

      // 将数据库行转为UserInfo对象
      auditors = rows.map { row =>
        UserInfo(
          userID = decodeField[Int](row, "user_id"),
          username = decodeField[String](row, "username"),
          avatarPath = decodeField[Option[String]](row, "avatar_path").getOrElse(""),
          isBanned = decodeField[Boolean](row, "is_banned")
        )
      }

      _ <- if (auditors.isEmpty)
             IO(logger.info(s"[Step 2.3] 未找到任何审核员，返回空列表"))
           else
             IO(logger.info(s"[Step 2.3] 找到${auditors.length}位审核员"))

    } yield Some(auditors)
  }
}