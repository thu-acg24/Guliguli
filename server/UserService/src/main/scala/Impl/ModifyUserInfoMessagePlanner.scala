package Impl


import Common.API.PlanContext
import Common.APIException.InvalidInputException
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserInfo
import Utils.AuthProcess.validateToken
import cats.effect.IO
import cats.implicits.*
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * Planner for ModifyUserInfoMessage: 根据用户Token校验身份后，更新用户表中newField对应字段的值，用于用户信息修改功能点
 */

case class ModifyUserInfoMessagePlanner(
                                         token: String,
                                         newField: UserInfo,
                                         override val planContext: PlanContext
                                       ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan definition
  override def plan(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Validating token $token"))
      userID <- validateToken(token)
      _ <- validateNewField(newField)
      _ <- updateUserInfoInDB(userID, newField)
    } yield ()
  }

  /**
   * Validate the fields in UserInfo (e.g., check username for proper format).
   */
  private def validateNewField(newField: UserInfo): IO[Unit] = {
    if (newField.username.length > 20 || newField.username.length < 3) {
      IO(logger.error(s"Username '${newField.username}' has invalid length")) >>
      IO.raiseError(InvalidInputException("用户名长度不正确"))
    } else {
      IO.unit
    }
  }

  /**
   * Update user information in the database.
   */
  private def updateUserInfoInDB(userID: Int, newField: UserInfo)(using PlanContext): IO[String] = {
    val querySQL =
      s"""
         UPDATE $schemaName.user_table
         SET username = ?, bio = ?, updated_at = ?
         WHERE user_id = ?
       """.stripMargin

    for {
      timestamp <- IO(DateTime.now().getMillis.toString)
      queryParams = List(
        SqlParameter("String", newField.username),
        SqlParameter("String", newField.bio),
        SqlParameter("DateTime", timestamp),
        SqlParameter("Int", userID.toString)
      )
      result <- IO(logger.info(s"Executing update query: $querySQL with params: $queryParams")) >>
        writeDB(querySQL, queryParams)
    } yield result
  }
}