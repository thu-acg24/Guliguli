package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Utils.AuthProcess.hashPassword
import Utils.AuthProcess.validatePassword
import Utils.AuthProcess.validateToken
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ModifyPasswordMessagePlanner(
                                         token: String,
                                         oldPassword: String,
                                         newPassword: String,
                                         override val planContext: PlanContext
                                       ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate token and get userID
      _ <- IO(logger.info(s"开始校验Token: ${token}"))
      userID <- validateToken(token)
      _ <- handlePasswordUpdate(userID)
    } yield ()
  }

  private def handlePasswordUpdate(userID: Int)(using PlanContext): IO[Unit] = {
    for {
      // Step 2: Validate old password
      oldPasswordValidation <- validateOldPassword(userID)
      newPasswordHash <- hashPassword(newPassword)
      _ <- updatePasswordHash(userID, newPasswordHash)
    } yield ()
  }

  private def validateOldPassword(userID: Int)(using PlanContext): IO[Unit] = IO {
      logger.info(s"开始验证用户ID为 ${userID} 的旧密码是否正确")
      validatePassword(userID, oldPassword)
  }

  private def updatePasswordHash(userID: Int, newPasswordHash: String)(using PlanContext): IO[String] = {
    val sql = s"""
          UPDATE ${schemaName}.user_table
          SET password_hash = ?, updated_at = NOW()
          WHERE user_id = ?;
        """
    val params =
      List(
        SqlParameter("String", newPasswordHash),
        SqlParameter("Int", userID.toString)
      )
    for {
      _ <- IO(logger.info(s"开始更新用户ID ${userID} 的密码哈希值"))
      updateResult <- writeDB(sql, params)
    } yield updateResult
  }
}