package Impl


/**
 * Planner for DeleteHistoryMessage. 根据用户Token校验后，从历史记录表删除指定记录。
 * 步骤如下：
 * 1. 校验用户的身份合法性.
 * 2. 删除指定的视频历史记录.
 * 3. 返回操作处理结果。
 */
import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import org.joda.time.DateTime
import cats.implicits._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
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
import APIs.UserService.getUIDByTokenMessage
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class DeleteHistoryMessagePlanner(
                                        token: String,
                                        videoID: Int,
                                        override val planContext: PlanContext
                                      ) extends Planner[Option[String]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Validate user identity
      userIDOpt <- validateUserIdentity(token)
      result <- userIDOpt match {
        case None =>
          // User validation failed
          IO(logger.info(s"Token validation failed for token: ${token}")) *> IO.pure(Some("Authentication failed: invalid token"))
        case Some(userID) =>
          // Step 2: Delete history record
          deleteHistoryRecord(userID, videoID)
      }
    } yield result
  }

  /**
   * 获取用户ID，并校验用户身份是否合法。
   *
   * @param token 用户的认证Token
   * @return IO[Option[Int]] 如果Token合法，返回Some(userID)，否则返回None
   */
  private def validateUserIdentity(token: String)(using PlanContext): IO[Option[Int]] = {
    for {
      _ <- IO(logger.info(s"Calling getUIDByTokenMessage with token: ${token}"))
      userIDOpt <- getUIDByTokenMessage(token).send
      _ <- IO(logger.info(s"Validation result for token '${token}': ${userIDOpt.fold("Invalid token")(userID => s"Valid UserID=$userID")}"))
    } yield userIDOpt
  }

  /**
   * 检查历史记录是否存在。
   *
   * @param userID 用户ID
   * @param videoID 视频ID
   * @return IO[Boolean] 返回记录是否存在
   */
  private def checkHistoryRecordExistence(userID: Int, videoID: Int)(using PlanContext): IO[Boolean] = {
    val sqlQuery =
      s"""
         |SELECT COUNT(*)
         |FROM ${schemaName}.history_record_table
         |WHERE user_id = ? AND video_id = ?;
         """.stripMargin
    val parameters = List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString)
    )

    for {
      _ <- IO(logger.info(s"Checking if history record exists for userID: ${userID}, videoID: ${videoID}"))
      count <- readDBInt(sqlQuery, parameters)
      exists <- IO(count > 0)
      _ <- IO(logger.info(s"History record existence check result for userID=${userID}, videoID=${videoID}: exists=${exists}"))
    } yield exists
  }

  /**
   * 删除指定的历史记录。
   *
   * @param userID 用户ID
   * @param videoID 视频ID
   * @return IO[Option[String]] None表示成功删除，否则返回错误信息。
   */
  private def deleteHistoryRecord(userID: Int, videoID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      exists <- checkHistoryRecordExistence(userID, videoID)
      result <- if (!exists) {
        IO(logger.info(s"No history record found for userID: ${userID}, videoID: ${videoID}")) *>
          IO.pure(Some("Record not found: the specified history record does not exist"))
      } else {
        val sqlQuery =
          s"""
             |DELETE FROM ${schemaName}.history_record_table
             |WHERE user_id = ? AND video_id = ?;
             """.stripMargin
        val parameters = List(
          SqlParameter("Int", userID.toString),
          SqlParameter("Int", videoID.toString)
        )
        for {
          _ <- IO(logger.info(s"Deleting history record for userID: ${userID}, videoID: ${videoID}"))
          _ <- writeDB(sqlQuery, parameters)
          _ <- IO(logger.info(s"Successfully deleted history record for userID: ${userID}, videoID: ${videoID}"))
        } yield None // Operation successful, return None
      }
    } yield result
  }
}