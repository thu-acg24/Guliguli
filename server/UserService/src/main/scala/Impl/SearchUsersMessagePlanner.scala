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

case class SearchUsersMessagePlanner(
    searchString: String,
    override val planContext: PlanContext
) extends Planner[List[UserInfo]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[UserInfo]] = {
    for {
      // Step 1: Query the UserTable for users whose username contains the search string
      _ <- IO(logger.info(s"[Step 1] 开始在UserTable中搜索用户名包含'${searchString}'的用户，限制前50个结果"))
      usersJson <- queryUsersWithSearchString(searchString)

      // Step 2: Parse the query results and convert to UserInfo objects
      _ <- IO(logger.info(s"[Step 2] 成功查询到用户数据，开始解析并按昵称排序"))
      result <- parseAndSortUserRecords(usersJson)
    } yield result
  }

  // Step 1: Query users whose username contains the search string
  private def queryUsersWithSearchString(searchString: String)(using PlanContext): IO[Json] = {
    for {
      _ <- IO(logger.info("[Step 1.1] 开始构造搜索用户的数据库指令，限制返回前50个结果"))
      sql <- IO {
        s"""
           |SELECT user_id, username, avatar_path, is_banned
           |FROM ${schemaName}.user_table
           |WHERE username LIKE ?
           |ORDER BY username ASC
           |LIMIT 50;
         """.stripMargin
      }
      _ <- IO(logger.info(s"[Step 1.2] 数据库指令为: ${sql}"))
      // Use LIKE pattern with % wildcards for substring search
      searchPattern <- IO(s"%${searchString}%")
      parameters <- IO(List(SqlParameter("String", searchPattern)))
      usersJson <- readDBJson(sql, parameters)
    } yield usersJson
  }

  // Step 2: Parse the user records and convert to UserInfo objects
  private def parseAndSortUserRecords(usersJson: Json)(using PlanContext): IO[List[UserInfo]] = {
    for {
      _ <- IO(logger.info("[Step 2.1] 开始解析数据库返回的用户列表"))
      usersList <- IO {
        usersJson.asArray.getOrElse(Vector.empty).toList
      }
      
      // Convert each user record to UserInfo
      userInfoList <- usersList.traverse(userJson => parseUserRecord(userJson))
      
      _ <- IO(logger.info(s"[Step 2.2] 成功解析${userInfoList.length}个用户信息"))
    } yield userInfoList
  }

  // Parse a single user record JSON to UserInfo object
  private def parseUserRecord(userJson: Json)(using PlanContext): IO[UserInfo] = {
    for {
      userID <- IO(decodeField[Int](userJson, "user_id"))
      username <- IO(decodeField[String](userJson, "username"))
      avatarPath <- IO(decodeField[String](userJson, "avatar_path"))
      isBanned <- IO(decodeField[Boolean](userJson, "is_banned"))

      // Get presigned URL for avatar
      avatarUrl <- IO.blocking { // 包装阻塞IO操作
        minioClient.getPresignedObjectUrl(
          io.minio.GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket("avatar")
            .`object`(avatarPath)
            .expiry(1, TimeUnit.MINUTES)
            .build()
        )
      }

      // Create UserInfo object
      userInfo <- IO {
        UserInfo(
          userID = userID,
          username = username,
          avatarPath = avatarUrl,
          isBanned = isBanned
        )
      }
    } yield userInfo
  }
}
