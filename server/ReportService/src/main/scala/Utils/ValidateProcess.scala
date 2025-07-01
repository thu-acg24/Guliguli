package Utils


import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.MessageService.Message
import Objects.UserService.UserRole
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

//process plan import 预留标志位，不要删除

case object ValidateProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除

  def validateTokenAndRole(
                                    token: String
                                  )(using PlanContext): IO[UserRole] = {
    for {
      _ <- IO(logger.info("开始校验 token 和用户角色"))
      userRole <- QueryUserRoleMessage(token).send
      _ <- userRole match {
        case UserRole.Auditor =>
          IO(logger.info("用户角色校验通过"))
        case _ =>
          IO(logger.error("用户角色校验失败")) >> IO.raiseError(SecurityException("用户无审核权限"))
      }
    } yield userRole
  }
}