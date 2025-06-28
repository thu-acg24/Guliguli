package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import cats.effect.IO
import org.mindrot.jbcrypt.BCrypt
import Common.API.PlanContext
import cats.implicits._
import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Utils.AuthProcess.hashPassword
import io.circe.Json
import java.util.UUID
import Common.API.{PlanContext}

case object AuthProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  
  def hashPassword(password: String)(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"开始生成密码的哈希值"))
      passwordHash <- IO {
        try {
          BCrypt.hashpw(password, BCrypt.gensalt())
        } catch {
          case e: Exception =>
            val errorMessage = s"哈希密码失败，错误信息: ${e.getMessage}"
            logger.error(errorMessage)
            throw new RuntimeException(errorMessage, e)
        }
      }
      _ <- IO(logger.info(s"密码哈希值生成成功，哈希值长度: ${passwordHash.length}"))
    } yield passwordHash
  }
  
  
  def invalidateToken(token: String)(using PlanContext): IO[Option[String]] = {
  // val logger = LoggerFactory.getLogger("TokenLogger")  // 同文后端处理: logger 统一
  
    val checkTokenSQL =
      s"SELECT token FROM ${schemaName}.token_table WHERE token = ?"
  
    val deleteTokenSQL =
      s"DELETE FROM ${schemaName}.token_table WHERE token = ?"
  
    val checkTokenParams = List(SqlParameter("String", token))
    val deleteTokenParams = List(SqlParameter("String", token))
  
    for {
      // Step 1: Check if the Token exists in TokenTable
      _ <- IO(logger.info(s"Checking if token '${token}' exists in the database."))
      tokenExists <- readDBJsonOptional(checkTokenSQL, checkTokenParams)
      result <- tokenExists match {
        case Some(_) =>
          IO(logger.info(s"Token '${token}' exists. Proceeding with deletion...")) >>
            writeDB(deleteTokenSQL, deleteTokenParams).attempt.flatMap {
              case Right(_) =>
                IO(logger.info(s"Token '${token}' successfully removed from TokenTable.")) >> IO(None)
              case Left(e) =>
                val errorMessage = s"Failed to delete Token '${token}' from TokenTable: ${e.getMessage}"
                IO(logger.error(errorMessage)) >> IO(Some("Database Error"))
            }
        case None =>
          val warningMessage = s"Token '${token}' does not exist in TokenTable."
          IO(logger.warn(warningMessage)) >> IO(Some("Session Not Found"))
      }
    } yield result
  }
  
  def validatePassword(userID: Int, inputPassword: String)(using PlanContext): IO[Option[String]] = {
  // val logger = LoggerFactory.getLogger("ValidatePassword")  // 同文后端处理: logger 统一
  
    val sql =
      s"""
        SELECT password_hash
        FROM ${schemaName}.user_table
        WHERE user_id = ?;
      """
  
    for {
      // Step 1: Attempt to retrieve the password hash from the database
      _ <- IO(logger.info(s"开始从数据库获取用户ID为${userID}的哈希密码"))
      resultOpt <- readDBJsonOptional(sql, List(SqlParameter("Int", userID.toString)))
      response <- resultOpt match {
        case None =>
          // Step 1.2: User ID not found
          val infoMessage = s"用户ID ${userID} 不存在"
          IO(logger.info(infoMessage)) *>
            IO.pure(Some("Invalid User"))
  
        case Some(json) =>
          // Step 2: Compare input password with stored hash
          for {
            // 2.1 Decode the stored hash from the database result
            storedHash <- IO(decodeField[String](json, "password_hash"))
  
            // Logging the retrieval of stored hash
            _ <- IO(logger.info(s"成功获取用户 ${userID} 的哈希密码，开始验证密码"))
  
            // 2.2 Hash the input password for comparison
            hashedInput <- hashPassword(inputPassword)
  
            // 2.3 Compare the hashed input password with the stored hash
            isMatch <- IO(storedHash == hashedInput)
  
            // Log the success or failure of the password match
            _ <- if (isMatch)
              IO(logger.info(s"用户ID ${userID} 的密码匹配成功"))
            else
              IO(logger.info(s"用户ID ${userID} 的密码验证失败"))
  
            // Prepare the response: None for success, Some("Invalid Password") for failure
            result <- IO(if (isMatch) None else Some("Invalid Password"))
  
          } yield result
      }
    } yield response
  }
  
  
  def validateToken(token: String)(using PlanContext): IO[Option[Int]] = {
  // val logger = LoggerFactory.getLogger(this.getClass)  // 同文后端处理: logger 统一
  
    // 日志信息，记录token校验开始
    IO(logger.info(s"开始校验Token: ${token}")) >>
    {
      val querySQL = 
        s"""
           SELECT user_id, expiration_time
           FROM ${schemaName}.token_table
           WHERE token = ?
         """
      val queryParams = List(SqlParameter("String", token))
  
      // 执行数据库查询
      for {
        _ <- IO(logger.info(s"[Step 1] 执行查询Token的用户信息与过期时间，SQL: ${querySQL}"))
        tokenRecord <- readDBJsonOptional(querySQL, queryParams)
  
        // 校验Token的有效性
        result <- tokenRecord match {
          case Some(record) =>
            for {
              userID <- IO(decodeField[Int](record, "user_id"))
              expirationTime <- IO(new DateTime(decodeField[Long](record, "expiration_time")))
              now <- IO(DateTime.now())
  
              validationResult <-
                if (now.isBefore(expirationTime)) {
                  IO {
                    logger.info(s"Token有效，返回用户ID: ${userID}")
                    Some(userID)
                  }
                } else {
                  IO {
                    logger.info(s"Token已过期，过期时间: ${expirationTime}, 当前时间: ${now}")
                    None
                  }
                }
            } yield validationResult
  
          case None =>
            IO {
              logger.info(s"Token不存在，返回None")
              None
            }
        }
      } yield result
    }
  }
  
  
  def generateToken(userID: Int)(using PlanContext): IO[Option[String]] = {
  // val logger = LoggerFactory.getLogger(this.getClass)  // 同文后端处理: logger 统一
  
    logger.info(s"开始生成Token，输入的userID为：${userID}")
  
    // 校验输入参数 userID 是否有效
    if (userID <= 0) {
      logger.error(s"userID 参数无效，值为：${userID}")
      IO.pure(None)
    } else {
      for {
        _ <- IO(logger.info(s"userID 参数校验通过，值为：${userID}"))
  
        // 生成用户 Token
        generatedToken <- IO {
          val randomPart = UUID.randomUUID().toString.replaceAll("-", "")
          val token = s"${randomPart}_${userID}"
          logger.info(s"生成的Token为：${token}")
          token
        }
  
        // 校验Token基本格式（假设唯一性和安全性在UUID层面已满足需求）
        _ <- IO(logger.info(s"对生成的Token基本校验完成：${generatedToken}"))
  
        // 准备保存到数据库
        expirationTime <- IO {
          val exp = new DateTime().plusHours(24) // 设置过期时间为24小时后
          logger.info(s"设置的Token过期时间为：${exp}")
          exp
        }
  
        writeParams <- IO {
          List(
            SqlParameter("String", generatedToken),
            SqlParameter("Int", userID.toString),
            SqlParameter("DateTime", expirationTime.getMillis.toString)
          )
        }
  
        writeSQL <- IO {
          s"""
            INSERT INTO ${schemaName}.token_table (token, user_id, expiration_time)
            VALUES (?, ?, ?);
          """
        }
  
        _ <- IO(logger.info(s"准备将生成的Token保存到数据库中，SQL为：${writeSQL}"))
  
        writeResult <- writeDB(writeSQL, writeParams).attempt
  
        result <- writeResult match {
          case Right(_) =>
            IO {
              logger.info(s"成功将Token保存到数据库，Token为：${generatedToken}")
              Some(generatedToken)
            }
          case Left(e) =>
            IO {
              logger.error(s"写入数据库时发生错误：${e.getMessage}")
              None
            }
        }
      } yield result
    }
  }
}
