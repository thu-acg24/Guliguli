package Impl

/**
 * Planner for ModifyUserInfoMessage: 根据用户Token校验身份后，更新用户表中newField对应字段的值，用于用户信息修改功能点
 */


import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.UserService.UserInfo
import Utils.AuthProcess.validateToken
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ModifyAvatarMessagePlanner(
                                         token: String,
                                         newField: UserInfo,
                                         override val planContext: PlanContext
                                       ) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan definition
  override def plan(using PlanContext): IO[List[String]] = {
    IO(logger.info(s"Validating token $token"))
    validateToken(token).flatMap {
      case Some(userID) =>
        ## TODO
      case None =>
        IO(logger.error("Token validation failed: Invalid Token")).as(List("", "Invalid Token"))
    }
  }
}