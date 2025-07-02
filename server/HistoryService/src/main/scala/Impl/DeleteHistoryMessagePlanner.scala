package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.APIException.InvalidInputException
import Common.API.PlanContext
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
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * Planner for DeleteHistoryMessage. 根据用户Token校验后，从历史记录表删除指定记录。
 * 步骤如下：
 * 1. 校验用户的身份合法性.
 * 2. 删除指定的视频历史记录.
 * 3. 返回操作处理结果。
 */

case class DeleteHistoryMessagePlanner(
                                        token: String,
                                        videoID: Int,
                                        override val planContext: PlanContext
                                      ) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate user identity
      userID <- validateUserIdentity(token)
      _ <- deleteHistoryRecord(userID, videoID)
    } yield ()
  }

  /**
   * 获取用户ID，并校验用户身份是否合法。
   *
   * @param token 用户的认证Token
   * @return IO[Int]
   */
  private def validateUserIdentity(token: String)(using PlanContext): IO[Int] = {
    for {
      _ <- IO(logger.info(s"Calling GetUIDByTokenMessage with token: ${token}"))
      userID <- GetUIDByTokenMessage(token).send
      _ <- IO(logger.info(s"Validation result for token ${token}"))
    } yield userID
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
   * @return IO[Unit]
   */
  private def deleteHistoryRecord(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    for {
      exists <- checkHistoryRecordExistence(userID, videoID)
      _ <- if (!exists) {
        IO(logger.info(s"No history record found for userID: ${userID}, videoID: ${videoID}"))*>
        IO.raiseError(InvalidInputException("Video not found"))
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
        } yield () // Operation successful
      }
    } yield ()
  }
}