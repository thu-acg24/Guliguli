package Utils


import Common.API.PlanContext
import Common.APIException.InvalidInputException
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import java.util.UUID
import org.joda.time.DateTime
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory

//process plan import 预留标志位，不要删除

case object AuthProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  
  def hashPassword(password: String)(using PlanContext): IO[String] = {
    for {
      passwordHash <- IO(BCrypt.hashpw(password, BCrypt.gensalt()))
      _ <- IO(logger.info(s"密码哈希值生成成功，哈希值长度: ${passwordHash.length}"))
    } yield passwordHash
  }
  
  def validatePassword(userID: Int, inputPassword: String)(using PlanContext): IO[Unit] = {
  // val logger = LoggerFactory.getLogger("ValidatePassword")  // 同文后端处理: logger 统一
    val sql =
      s"""
        SELECT password_hash
        FROM $schemaName.user_table
        WHERE user_id = ?;
      """
    for {
      // Step 1: Attempt to retrieve the password hash from the database
      _ <- IO(logger.info(s"开始从数据库获取用户ID为${userID}的哈希密码"))
      resultOpt <- readDBJsonOptional(sql, List(SqlParameter("Int", userID.toString)))
      json <- resultOpt match {
        case None =>
          IO(logger.info(s"用户ID ${userID} 不存在")) *>
          IO.raiseError(new InvalidInputException("用户不存在？"))
        case Some(json) => IO(json)
      }
      storedHash <- IO(decodeField[String](json, "password_hash"))
      _ <- IO(logger.info(s"成功获取用户 ${userID} 的哈希密码，开始验证密码"))
      isMatch <- IO(BCrypt.checkpw(inputPassword, storedHash))
      _ <- if (isMatch) {
        IO(logger.info(s"用户ID ${userID} 的密码匹配成功"))
      } else {
        IO(logger.info(s"用户ID ${userID} 的密码验证失败")) *>
        IO.raiseError(new InvalidInputException(s"用户密码验证失败"))
      }
    } yield()
  }
  
  
  def validateToken(token: String)(using PlanContext): IO[Int] = {
  // val logger = LoggerFactory.getLogger(this.getClass)  // 同文后端处理: logger 统一
    val querySQL =
        s"""
           |SELECT user_id, expiration_time
           |FROM $schemaName.token_table
           |WHERE token = ?
         """.stripMargin
    val queryParams = List(SqlParameter("String", token))
    // 日志信息，记录token校验开始
    for {
      _ <- IO(logger.info(s"开始校验Token: ${token}"))
      _ <- IO(logger.info(s"[Step 1] 执行查询Token的用户信息与过期时间，SQL: ${querySQL}"))
      record <- readDBJsonOptional(querySQL, queryParams).flatMap {
          case Some(record) => IO(record)
          case None =>
            IO(logger.info(s"查询的Token ${token}不存在")) *>
            IO.raiseError(new InvalidInputException(s"Token不存在"))
        }
      userID <- IO(decodeField[Int](record, "user_id"))
      expirationTime <- IO(new DateTime(decodeField[Long](record, "expiration_time")))
      now <- IO(DateTime.now())
      result <- if (now.isBefore(expirationTime)) {
          IO(logger.info(s"Token有效，返回用户ID: ${userID}")).as(userID)
        } else {
          val deleteTokenSQL = s"DELETE FROM $schemaName.token_table WHERE token = ?"
          val deleteTokenParams = List(SqlParameter("String", token))
          IO(logger.info(s"Token已过期，过期时间: ${expirationTime}, 当前时间: ${now}")) *>
          writeDB(deleteTokenSQL, deleteTokenParams).attempt.flatMap {
            case Right(_) =>
              IO(logger.info(s"Outdated token '${token}' successfully removed from TokenTable."))
            case Left(e) =>
              val errorMessage = s"Failed to delete outdated token '${token}' from TokenTable: ${e.getMessage}"
              IO(logger.error(errorMessage))
          } *>
          IO.raiseError(new InvalidInputException(s"Token已过期，请重新登录"))
        }
    } yield result
  }
  
  
  def generateToken(userID: Int)(using PlanContext): IO[String] = {
  // val logger = LoggerFactory.getLogger(this.getClass)  // 同文后端处理: logger 统一
    val expirationTime = new DateTime().plusHours(24)
    val randomPart = UUID.randomUUID().toString
    val generatedToken = s"${randomPart}_${userID}"
    val writeSQL = s"""
      |INSERT INTO $schemaName.token_table (token, user_id, expiration_time)
      |VALUES (?, ?, ?);
      """.stripMargin
    val writeParams = List(
      SqlParameter("String", generatedToken),
      SqlParameter("Int", userID.toString),
      SqlParameter("DateTime", expirationTime.getMillis.toString)
    )
    for {
      _ <- IO(logger.info(s"开始生成Token，输入的userID为：${userID}"))
      _ <- IO(logger.info(s"生成的Token为：${generatedToken}"))
      _ <- IO(logger.info(s"设置的Token过期时间为：$expirationTime"))
      _ <- IO(logger.info(s"准备将生成的Token保存到数据库中，SQL为：$writeSQL"))
      _ <- writeDB(writeSQL, writeParams)
      _ <- IO(logger.info(s"成功将Token保存到数据库，Token为：$generatedToken"))
    } yield generatedToken
  }
}