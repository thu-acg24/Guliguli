package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserInfo
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryUserInfoMessagePlanner(
    userID: Int,
    override val planContext: PlanContext
) extends Planner[UserInfo] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[UserInfo] = {
    for {
      // Step 1: Query the UserTable for the specific userID
      _ <- IO(logger.info(s"[Step 1] 开始在UserTable查询userID为${userID}的用户基本信息"))
      userJson <- queryUserRecord(userID)

      // Step 2: Handle the query result and validate user's ban status
      _ <- IO(logger.info(s"[Step 1.2] 成功查询到userID为${userID}的用户信息，开始解析"))
      result <- validateAndWrapUserRecord(userJson)
    } yield result
  }

  // Step 1: Query the user record from the database
  private def queryUserRecord(userID: Int)(using PlanContext): IO[Json] = {
    for {
      _ <- IO(logger.info("[Step 1.1.1] 开始构造查询用户信息的数据库指令"))
      sql <- IO {
        s"""
           |SELECT user_id, username, avatar_path, is_banned
           |FROM ${schemaName}.user_table
           |WHERE user_id = ?;
         """.stripMargin
      }
      _ <- IO(logger.info(s"[Step 1.1.2] 数据库指令为: ${sql}"))
      parameters <- IO(List(SqlParameter("Int", userID.toString)))
      userJson <- readDBJson(sql, parameters)
    } yield userJson
  }

  // Step 2: Validate and wrap the user record
  private def validateAndWrapUserRecord(userJson: Json)(using PlanContext): IO[UserInfo] = {
    for {
      _ <- IO(logger.info("[Step 2.1] 开始解析数据库返回的用户信息"))
      userID <- IO(decodeField[Int](userJson, "user_id"))
      username <- IO(decodeField[String](userJson, "username"))
      avatarPath <- IO(decodeField[String](userJson, "avatar_path"))
      isBanned <- IO(decodeField[Boolean](userJson, "is_banned"))

      // Wrap the parsed data into UserInfo object
      userInfo <- IO {
        val userInfo = UserInfo(
          userID = userID,
          username = username,
          avatarPath = avatarPath,
          isBanned = isBanned
        )
        logger.info(s"[Step 2.3] 封装成UserInfo对象: ${userInfo}")
        userInfo
      }
    } yield userInfo
  }
}