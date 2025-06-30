package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserInfoMessage
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
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryUserInContactMessagePlanner(
                                             token: String,
                                             override val planContext: PlanContext
                                           ) extends Planner[List[UserInfo]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[UserInfo]] = {
    for {
      // Step 1: Validate token and retrieve userID
      _ <- IO(logger.info(s"验证Token获取用户ID"))
      maybeUserID <- validateAndRetrieveUserID()
      result <- maybeUserID match {
        case Some(userID) =>
          for {
            _ <- IO(logger.info(s"Token验证成功, userID=${userID}"))
            // Step 2: Retrieve contact user IDs
            contactUserIDs <- retrieveContactUserIDs(userID)
            _ <- IO(logger.info(s"与当前用户有联系的用户ID列表: ${contactUserIDs}"))
            // Step 3: Query contact user information
            contacts <- retrieveContactUserInfo(contactUserIDs)
          } yield contacts
        case None =>
          logger.info("Token无效，返回空列表")
          IO.pure(List.empty[UserInfo])
      }
    } yield result
  }

  private def validateAndRetrieveUserID()(using PlanContext): IO[Option[Int]] = {
    GetUIDByTokenMessage(token).send
  }

  private def retrieveContactUserIDs(userID: Int)(using PlanContext): IO[List[Int]] = {
    logger.info("开始创建获取联系人ID的数据库指令")
    val sql =
      s"""
        SELECT DISTINCT CASE
          WHEN sender_id = ? THEN receiver_id
          WHEN receiver_id = ? THEN sender_id
        END AS contact_user_id
        FROM ${schemaName}.message_table
        WHERE (sender_id = ? OR receiver_id = ?) AND is_notification = false;
      """
    logger.info(s"指令为${sql}")
    readDBRows(sql, List.fill(4)(SqlParameter("Int", userID.toString))).map {
      rows => rows.map(json => decodeField[Int](json, "contact_user_id"))
    }
  }

  private def retrieveContactUserInfo(contactUserIDs: List[Int])(using PlanContext): IO[List[UserInfo]] = {
    logger.info("开始根据联系人ID获取用户信息")
    contactUserIDs.traverse { id =>
      QueryUserInfoMessage(id).send.map {
        case Some(userInfo) =>
          logger.info(s"成功获取用户信息: ${userInfo}")
          Some(userInfo)
        case None =>
          logger.info(s"无法获取用户信息, userID=${id}")
          None
      }
    }.map(_.flatten)
  }
}