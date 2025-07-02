package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.MessageService.UserInfoWithMessage
import Objects.UserService.UserInfo
import cats.effect.IO
import cats.implicits.*
import cats.syntax.traverse._
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryUserInContactMessagePlanner(
                                             token: String,
                                             override val planContext: PlanContext
                                           ) extends Planner[List[UserInfoWithMessage]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[UserInfoWithMessage]] = {
    for {
      // Step 1: Validate token and retrieve userID
      _ <- IO(logger.info(s"验证Token获取用户ID"))
      userID <- validateAndRetrieveUserID()
      _ <- IO(logger.info(s"Token验证成功, userID=${userID}"))
      // Step 2: Retrieve contact user IDs
      contactUserIDs <- retrieveContactUserIDs(userID)
      _ <- IO(logger.info(s"与当前用户有联系的用户ID列表: ${contactUserIDs}"))
      // Step 3: Query contact user information
      userInfo <- retrieveContactUserInfo(contactUserIDs)
      userWithMessage <- userInfo.traverse(combineInformation)
    } yield userWithMessage
  }

  private def validateAndRetrieveUserID()(using PlanContext): IO[Int] = {
    GetUIDByTokenMessage(token).send
  }

  private def retrieveContactUserIDs(userID: Int)(using PlanContext): IO[List[Int]] = {
    val sql =
      s"""
          |SELECT DISTINCT CASE
          |WHEN sender_id = ? THEN receiver_id
          |WHEN receiver_id = ? THEN sender_id
          |END AS contact_user_id
          |FROM ${schemaName}.message_table
          |WHERE (sender_id = ? OR receiver_id = ?)
      """.stripMargin
    IO(logger.info("开始创建获取联系人ID的数据库指令"))*>
    IO(logger.info(s"指令为${sql}")) *>
    readDBRows(sql, List.fill(4)(SqlParameter("Int", userID.toString))).map {
      rows => rows.map(json => decodeField[Int](json, "contact_user_id"))
    }
  }

  private def retrieveContactUserInfo(contactUserIDs: List[Int])(using PlanContext): IO[List[UserInfo]] = {
    IO(logger.info("开始根据联系人ID获取用户信息"))
    contactUserIDs.traverse { id =>
      QueryUserInfoMessage(id).send.map { userInfo =>
        logger.info(s"成功获取用户信息: ${userInfo}")
        userInfo
      }
    }
  }

  private def combineInformation(userInfo: UserInfo)(using PlanContext): IO[UserInfoWithMessage] = {
    val countSql =
      s"""
         |SELECT COUNT(*) AS unread_count
         |FROM $schemaName.message_table
         |WHERE sender_id = ?
         |AND unread = True
      """.stripMargin
    val getLastSql =
      s"""
         |SELECT content, send_time
         |FROM $schemaName.message_table
         |WHERE (receiver_id=? OR sender_id=?)
         |ORDER BY send_time
         |DESC LIMIT 1
      """.stripMargin
    val parameter = List(SqlParameter("Int", userInfo.userID.toString))
    for {
      _ <- IO(logger.info("查询信息数量Sql: $countSql"))
      count <- readDBInt(countSql, parameter)
      json <- readDBJson(getLastSql, parameter.flatMap(x => List(x, x)))
      result <- IO(UserInfoWithMessage(
        userInfo = userInfo,
        unreadCount = count,
        timestamp = decodeField[DateTime](json, "send_time"),
        content = decodeField[String](json, "content"),
      ))
    } yield result
  }
}