package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.ParameterList
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Utils.AuthProcess.hashPassword
import cats.effect.IO
import cats.implicits.*
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class RegisterMessagePlanner(
  username: String,
  email: String,
  password: String,
  override val planContext: PlanContext
) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {

    for {
      // Step 1: Validate format of username and email
      _ <- IO(logger.info("开始校验用户名与邮箱格式"))
      isUsernameValid <- validateUsernameFormat(username)
      isEmailValid <- validateEmailFormat(email)
      _ <- if (!isUsernameValid || !isEmailValid) 
             IO.raiseError(new Exception("Invalid Username or Email Format")) 
           else 
             IO(logger.info("用户名与邮箱格式校验通过"))

      // Step 2: Check for username and email existence
      _ <- IO(logger.info("检查用户名与邮箱是否已存在"))
      isExist <- checkUserExistence(username, email)
      _ <- if (isExist) 
             IO.raiseError(new Exception("Username or Email already in use")) 
           else 
             IO(logger.info("用户名与邮箱检查通过"))

      // Step 3: Hash the password
      _ <- IO(logger.info("对密码进行哈希处理"))
      passwordHash <- hashPassword(password).handleErrorWith { e =>
        IO {
          logger.error(s"密码哈希失败，错误信息：${e.getMessage}")
          throw e
        }
      }
      _ <- IO(logger.info("密码哈希处理成功"))

      // Step 4: Insert user record into UserTable
      _ <- IO(logger.info("准备将用户信息插入数据库"))
      result <- insertUserRecord(username, email, passwordHash)
      _ <- if (result) 
             IO(logger.info("用户注册成功")) 
           else 
             IO.raiseError(new Exception("Failed to register user"))

    } yield None
  }.handleErrorWith { e =>
    IO {
      logger.error(s"用户注册失败，错误信息：${e.getMessage}")
      Some(e.getMessage)
    }
  }

  // Validate username format
  private def validateUsernameFormat(username: String): IO[Boolean] = IO {
    logger.info(s"校验用户名格式: ${username}")
    username.nonEmpty && username.length >= 3 && username.matches("^[a-zA-Z0-9_]+$")
  }

  // Validate email format
  private def validateEmailFormat(email: String): IO[Boolean] = IO {
    logger.info(s"校验邮箱格式: ${email}")
    email.nonEmpty && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
  }

  // Check if username or email already exists
  private def checkUserExistence(username: String, email: String)(using PlanContext): IO[Boolean] = {
    val query =
      s"""
        SELECT 1
        FROM ${schemaName}.user_table
        WHERE username = ? OR email = ?;
      """
    readDBJsonOptional(query, List(
      SqlParameter("String", username),
      SqlParameter("String", email)
    )).map(_.isDefined)
  }

  // Insert the user record into UserTable
  private def insertUserRecord(username: String, email: String, passwordHash: String)(using PlanContext): IO[Boolean] = {
    val now = DateTime.now()
    val query =
      s"""
        INSERT INTO ${schemaName}.user_table (username, email, password_hash, user_role, created_at, updated_at)
        VALUES (?, ?, ?, 'Normal', ?, ?);
      """
    writeDB(query, List(
      SqlParameter("String", username),
      SqlParameter("String", email),
      SqlParameter("String", passwordHash),
      SqlParameter("DateTime", now.getMillis.toString),
      SqlParameter("DateTime", now.getMillis.toString)
    )).map(_ => true).handleErrorWith { e =>
      IO {
        logger.error(s"插入数据库失败，错误信息：${e.getMessage}")
        false
      }
    }
  }
}