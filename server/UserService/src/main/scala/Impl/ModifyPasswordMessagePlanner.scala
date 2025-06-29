package Impl


import Utils.AuthProcess.validatePassword
import Utils.AuthProcess.hashPassword
import Utils.AuthProcess.validateToken
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits._
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
import Utils.AuthProcess.validateToken
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ModifyPasswordMessagePlanner(
                                         token: String,
                                         oldPassword: String,
                                         newPassword: String,
                                         override val planContext: PlanContext
                                       ) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Validate token and get userID
      _ <- IO(logger.info(s"开始校验Token: ${token}"))
      maybeUserID <- validateToken(token)
      result <- maybeUserID match {
        case None =>
          // Token validation failed, return error
          IO(logger.info(s"Token验证失败，无法获取用户ID")) *> IO.pure(Some("Invalid or Expired Token"))

        case Some(userID) =>
          IO(logger.info(s"Token验证成功，对应的用户ID: ${userID}")) *>
            handlePasswordUpdate(userID)
      }
    } yield result
  }

  private def handlePasswordUpdate(userID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      // Step 2: Validate old password
      oldPasswordValidation <- validateOldPassword(userID)
      result <- oldPasswordValidation match {
        case Some(errInfo) =>
          // Old password validation failed
          IO(logger.info(s"旧密码验证失败，错误信息: ${errInfo}")) *> IO.pure(Some(errInfo))

        case None =>
          // Old password is valid, proceed to hash new password
          hashNewPassword.flatMap {
            case Left(errInfo) =>
              IO(logger.info(s"新密码生成哈希失败，错误信息: ${errInfo}")) *> IO.pure(Some(errInfo))

            case Right(newPasswordHash) =>
              // Step 4: Update User Table with new hash
              updatePasswordHash(userID, newPasswordHash)
          }
      }
    } yield result
  }

  private def validateOldPassword(userID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"开始验证用户ID为 ${userID} 的旧密码是否正确"))
      result <- validatePassword(userID, oldPassword)
    } yield result
  }

  private def hashNewPassword(using PlanContext): IO[Either[String, String]] = {
    for {
      _ <- IO(logger.info(s"开始生成新密码的哈希值"))
      result <- hashPassword(newPassword).attempt.map {
        case Left(e) =>
          val errorMessage = s"生成新密码哈希值失败，错误信息: ${e.getMessage}"
          logger.error(errorMessage)
          Left("Failed to hash new password")

        case Right(newPasswordHash) =>
          logger.info(s"新密码哈希值生成成功")
          Right(newPasswordHash)
      }
    } yield result
  }

  private def updatePasswordHash(userID: Int, newPasswordHash: String)(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"开始更新用户ID ${userID} 的密码哈希值"))
      sql <-
        IO.pure(s"""
          UPDATE ${schemaName}.user_table
          SET password_hash = ?, updated_at = NOW()
          WHERE user_id = ?;
        """)
      params <- IO.pure(
        List(
          SqlParameter("String", newPasswordHash),
          SqlParameter("Int", userID.toString)
        )
      )

      updateResult <- writeDB(sql, params).attempt.map {
        case Left(e) =>
          val errorMessage = s"更新用户ID ${userID} 的密码哈希时出错，错误信息: ${e.getMessage}"
          logger.error(errorMessage)
          Some("Failed to modify user password")

        case Right(_) =>
          logger.info(s"用户ID ${userID} 的密码更新成功")
          None
      }
    } yield updateResult
  }
}