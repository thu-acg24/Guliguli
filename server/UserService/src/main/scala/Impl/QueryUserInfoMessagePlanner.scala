package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.minioClient
import Objects.UserService.UserInfo
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.http.Method
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

case class QueryUserInfoMessagePlanner(
    userID: Int,
    override val planContext: PlanContext
) extends Planner[UserInfo] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[UserInfo] = {
    for {
      // Step 1: Query the UserTable for the specific userID
      _ <- IO(logger.info(s"[Step 1] 开始在UserTable查询userID为$userID的用户基本信息"))
      userJson <- queryUserRecord(userID)

      // Step 2: Handle the query result and validate user's ban status
      _ <- IO(logger.info(s"[Step 1.2] 成功查询到userID为$userID的用户信息，开始解析"))
      result <- validateAndWrapUserRecord(userJson)
    } yield result
  }

  // Step 1: Query the user record from the database
  private def queryUserRecord(userID: Int)(using PlanContext): IO[Json] = {
    for {
      _ <- IO(logger.info("[Step 1.1.1] 开始构造查询用户信息的数据库指令"))
      sql <- IO {
        s"""
           |SELECT user_id, username, avatar_path, is_banned, bio
           |FROM $schemaName.user_table
           |WHERE user_id = ?;
         """.stripMargin
      }
      _ <- IO(logger.info(s"[Step 1.1.2] 数据库指令为: $sql"))
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
      bio <- IO(decodeField[String](userJson, "bio"))
      isBanned <- IO(decodeField[Boolean](userJson, "is_banned"))

      _ <- IO(logger.info("[Step 2.2] 开始获取头像的minIO自签名url"))
      avatarUrl <- IO.blocking { // 包装阻塞IO操作
        minioClient.getPresignedObjectUrl(
          io.minio.GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket("avatar")
            .`object`(avatarPath)
            .expiry(1, TimeUnit.DAYS)
            .build()
        )
      }

      // Wrap the parsed data into UserInfo object
      userInfo <- IO {
        val userInfo = UserInfo(
          userID = userID,
          username = username,
          avatarPath = avatarUrl,
          bio = bio,
          isBanned = isBanned
        )
        logger.info(s"[Step 2.3] 封装成UserInfo对象: $userInfo")
        userInfo
      }
    } yield userInfo
  }
}