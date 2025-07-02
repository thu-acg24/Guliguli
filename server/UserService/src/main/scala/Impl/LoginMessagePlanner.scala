package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.APIException.InvalidInputException
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Utils.AuthProcess.generateToken
import Utils.AuthProcess.hashPassword
import Utils.AuthProcess.validatePassword
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class LoginMessagePlanner(
  usernameOrEmail: String,
  password: String,
  override val planContext: PlanContext
) extends Planner[String] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Query user information from UserTable
      _ <- IO(logger.info(s"查询用户信息，输入的usernameOrEmail为：${usernameOrEmail}"))
      userJson <- getUserByUsernameOrEmail()
      userID <- IO(decodeField[Int](userJson, "user_id"))
      // Step 2: Validate the password
      _ <- IO(logger.info(s"开始验证用户ID为${userID}的密码"))
      _ <- validatePassword(userID, password)
      _ <- IO(logger.info(s"检查用户是否被封禁，用户ID为$userID"))
      isBanned <- IO(decodeField[Boolean](userJson, "is_banned"))
      response <- if (isBanned) {
        IO(logger.info("用户账户已被封禁")) *>
        IO.raiseError(new InvalidInputException("用户已被封禁"))
      } else {
        generateTokenResponse(userID)
      }
    } yield response
  }

  private def getUserByUsernameOrEmail()(using PlanContext): IO[Json] = {
    val sql =
      s"""
        SELECT user_id, password_hash, is_banned
        FROM ${schemaName}.user_table
        WHERE username = ? OR email = ?;
      """
    val params = List(
      SqlParameter("String", usernameOrEmail),
      SqlParameter("String", usernameOrEmail)
    )

    readDBJsonOptional(sql, params)
      .flatMap {
        case None =>
          IO(logger.info(s"未找到用户信息，登陆凭证 $usernameOrEmail")) *>
          IO.raiseError(InvalidInputException(s"未找到用户信息"))
        case Some(json) =>
          IO.pure(json)
      }
  }

  private def generateTokenResponse(userID: Int)(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"为用户ID ${userID} 生成Token"))
      token <- generateToken(userID)
      response <- IO(logger.info(s"Token生成成功, Token为：${token}")).as(token)
    } yield response
  }
}