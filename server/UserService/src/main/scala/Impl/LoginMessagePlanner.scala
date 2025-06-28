package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.AuthProcess.{generateToken, validatePassword}
import cats.effect.IO
import io.circe.Json
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
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
import Utils.AuthProcess.validatePassword
import Utils.AuthProcess.generateToken
import Utils.AuthProcess.hashPassword
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Utils.AuthProcess.hashPassword

case class LoginMessagePlanner(
  usernameOrEmail: String,
  password: String,
  override val planContext: PlanContext
) extends Planner[List[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[String]] = {
    for {
      // Step 1: Query user information from UserTable
      _ <- IO(logger.info(s"查询用户信息，输入的usernameOrEmail为：${usernameOrEmail}"))
      userOpt <- getUserByUsernameOrEmail()
      response <- userOpt match {
        case None =>
          IO(logger.info("用户未找到")).as(List("", "User not found"))

        case Some(userJson) =>
          for {
            userID <- IO(decodeField[Int](userJson, "user_id"))
            passwordHash <- IO(decodeField[String](userJson, "password_hash"))

            // Step 2: Validate the password
            _ <- IO(logger.info(s"开始验证用户ID为${userID}的密码"))
            isPasswordValid <- validatePassword(userID, password).map(_.isEmpty) // None indicates success
            _ <- IO(logger.info(s"密码验证结果：${isPasswordValid}"))

            response <- if (!isPasswordValid) {
              IO(logger.info("密码验证失败")).as(List("", "Invalid login credentials"))
            } else {

              // Step 3: Check if the user is banned
              _ <- IO(logger.info(s"检查用户是否被封禁，用户ID为${userID}"))
              isBanned <- IO(decodeField[Boolean](userJson, "is_banned"))
              _ <- IO(logger.info(s"用户封禁状态：${isBanned}"))

              if (isBanned) {
                IO(logger.info("用户账户已被封禁")).as(List("", "Account has been banned"))
              } else {
                // Step 4: Generate Token
                generateTokenResponse(userID)
              }
            }
          } yield response
      }
    } yield response
  }

  private def getUserByUsernameOrEmail()(using PlanContext): IO[Option[Json]] = {
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
  }

  private def generateTokenResponse(userID: Int)(using PlanContext): IO[List[String]] = {
    for {
      _ <- IO(logger.info(s"为用户ID ${userID} 生成Token"))
      tokenOpt <- generateToken(userID)
      response <- tokenOpt match {
        case None =>
          IO(logger.info("Token生成失败")).as(List("", "Token generation failed"))
        case Some(token) =>
          IO(logger.info(s"Token生成成功, Token为：${token}")).as(List(token, ""))
      }
    } yield response
  }
}