package Impl


import Utils.AuthProcess.validateToken
import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.{Logger, LoggerFactory}
import org.joda.time.DateTime
import io.circe.Json
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI.*
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Utils.AuthProcess.validateToken
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}

case class ChangeFollowStatusMessagePlanner(
                                             token: String,
                                             followeeID: Int,
                                             isFollow: Boolean,
                                             override val planContext: PlanContext
                                           ) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"Start processing with token=$token, followeeID=$followeeID, isFollow=$isFollow"))

      // Step 1: 验证Token
      userIdOrError <- validateToken(token).map {
        case Some(id) => Right(id)
        case None => Left(Some("Invalid Token"))
      }

      // 提前处理token错误
      result <- userIdOrError match {
        case Left(error) =>
          IO(logger.info("Invalid token")).as(error)

        case Right(userId) =>
          for {
            // Step 2: 检查用户存在
            exists <- checkUserExists(followeeID)

            // 处理用户不存在的情况
            result2 <- if (!exists) {
              IO(logger.info("Target user not found")).as(Some("Target user not found"))
            } else {
              // Step 3: 执行操作
              if (isFollow) handleFollow(userId, followeeID)
              else handleUnfollow(userId, followeeID)
            }
          } yield result2
      }
    } yield result
  }

  private def checkUserExists(userID: Int)(using PlanContext): IO[Boolean] = {
    logger.info(s"Checking if user with ID=${userID} exists.")
    val sql =
      s"""
         SELECT 1
         FROM ${schemaName}.user_table
         WHERE user_id = ?;
       """
    readDBBoolean(sql, List(SqlParameter("Int", userID.toString)))
      .map(exists => {
        logger.info(s"User existence status for userID=${userID}: ${exists}")
        exists
      })
  }

  private def handleFollow(followerID: Int, followeeID: Int)(using PlanContext): IO[Option[String]] = {
    logger.info(s"Handling follow action for followerID=${followerID} and followeeID=${followeeID}.")

    val checkSQL =
      s"""
         SELECT 1
         FROM ${schemaName}.follow_relation_table
         WHERE follower_id = ? AND followee_id = ?;
       """
    val insertSQL =
      s"""
         INSERT INTO ${schemaName}.follow_relation_table (follower_id, followee_id, timestamp)
         VALUES (?, ?, ?);
       """
    val params = List(SqlParameter("Int", followerID.toString), SqlParameter("Int", followeeID.toString))

    for {
      alreadyExists <- readDBBoolean(checkSQL, params)
      result <- if (alreadyExists) {
        logger.info(s"Follow relation already exists between followerID=${followerID} and followeeID=${followeeID}.")
        IO.pure(Some("Already following the user"))
      } else {
        logger.info(s"Inserting new follow relation for followerID=${followerID} and followeeID=${followeeID}.")
        val timestamp = DateTime.now().getMillis.toString
        writeDB(insertSQL, params :+ SqlParameter("DateTime", timestamp)).map(_ => None)
      }
    } yield result
  }

  private def handleUnfollow(followerID: Int, followeeID: Int)(using PlanContext): IO[Option[String]] = {
    logger.info(s"Handling unfollow action for followerID=${followerID} and followeeID=${followeeID}.")

    val checkSQL =
      s"""
         SELECT 1
         FROM ${schemaName}.follow_relation_table
         WHERE follower_id = ? AND followee_id = ?;
       """
    val deleteSQL =
      s"""
         DELETE FROM ${schemaName}.follow_relation_table
         WHERE follower_id = ? AND followee_id = ?;
       """
    val params = List(SqlParameter("Int", followerID.toString), SqlParameter("Int", followeeID.toString))

    for {
      exists <- readDBBoolean(checkSQL, params)
      result <- if (!exists) {
        logger.info(s"No follow relation exists between followerID=${followerID} and followeeID=${followeeID}.")
        IO.pure(Some("Not following the user"))
      } else {
        logger.info(s"Deleting follow relation between followerID=${followerID} and followeeID=${followeeID}.")
        writeDB(deleteSQL, params).map(_ => None)
      }
    } yield result
  }
}