package Impl

/**
 * Planner for ModifyUserInfoMessage: 根据用户Token校验身份后，更新用户表中newField对应字段的值，用于用户信息修改功能点
 */


import Objects.UserService.UserInfo
import Utils.AuthProcess.validateToken
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
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
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ModifyUserInfoMessagePlanner(
                                         token: String,
                                         newField: UserInfo,
                                         override val planContext: PlanContext
                                       ) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan definition
  override def plan(using PlanContext): IO[Option[String]] = {
    IO(logger.info(s"Validating token $token"))
    validateToken(token).flatMap {
      case Some(userID) =>
        validateNewField(newField) match {
          case Some(error) =>
            IO(logger.error(s"Field validation failed: $error")) *>
              IO.pure(Some(error))

          case None =>
            for {
              _ <- IO(logger.info("All validations passed, updating database"))
              result <- updateUserInfoInDB(userID, newField)
            } yield result
        }

      case None =>
        IO(logger.error("Token validation failed: Invalid Token")) *>
          IO.pure(Some("Invalid Token"))
    }
  }

  /**
   * Validate the fields in UserInfo (e.g., check username and avatarPath for proper format).
   */
  private def validateNewField(newField: UserInfo): Option[String] = {
    if (newField.username.length > 20) {
      logger.error(s"Username '${newField.username}' exceeds max length of 20")
      Some("Invalid Field Value: Username exceeds max length of 20")
    } else if (!newField.avatarPath.matches("^[a-zA-Z0-9/_-]*$")) { // Example regex for avatar validation
      logger.error(s"Avatar path '${newField.avatarPath}' contains invalid characters")
      Some("Invalid Field Value: Avatar path format error")
    } else {
      None
    }
  }

  /**
   * Update user information in the database.
   */
  private def updateUserInfoInDB(userID: Int, newField: UserInfo)(using PlanContext): IO[Option[String]] = {
    val querySQL =
      s"""
         UPDATE ${schemaName}.user_table
         SET username = ?, avatar_path = ?, is_banned = ?, updated_at = ?
         WHERE user_id = ?
       """.stripMargin

    val queryParams = List(
      SqlParameter("String", newField.username),
      SqlParameter("String", newField.avatarPath),
      SqlParameter("Boolean", newField.isBanned.toString),
      SqlParameter("DateTime", DateTime.now().getMillis.toString),
      SqlParameter("Int", userID.toString)
    )

    for {
      _ <- IO(logger.info(s"Executing update query: $querySQL with params: $queryParams"))
      updateResponse <- writeDB(querySQL, queryParams)

      // Check the result of the database operation
      result <- {
        if (updateResponse.contains("Operation(s) done successfully")) {
          IO {
            logger.info("User information updated successfully.")
            None // No error, meaning success
          }
        } else {
          IO {
            logger.error("Failed to modify user info in the database.")
            Some("Failed to modify user info")
          }
        }
      }
    } yield result
  }
}