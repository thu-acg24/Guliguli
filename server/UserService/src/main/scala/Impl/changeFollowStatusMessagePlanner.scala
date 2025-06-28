package Impl


import Utils.AuthProcess.validateToken
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe.Json
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
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
import Utils.AuthProcess.validateToken
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ChangeFollowStatusMessagePlanner(
                                             token: String,
                                             followeeID: Int,
                                             isFollow: Boolean,
                                             override val planContext: PlanContext
                                           ) extends Planner[Option[String]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"Start processing changeFollowStatusMessage with token=${token}, followeeID=${followeeID}, isFollow=${isFollow}"))

      // Step 1: Validate Token and obtain userID
      userIDOpt <- validateToken(token)
      userID <- userIDOpt match {
        case Some(id) => IO.pure(id)
        case None =>
          IO {
            logger.info("Invalid token provided.")
            Some("Invalid Token")
          }
      }

      // Step 2: Check if target user exists
      targetExists <- checkUserExists(followeeID)
      _ <- if (!targetExists) IO {
        logger.info("Target user not found, returning error.")
        Some("Target user not found")
      } else IO.unit

      // Step 3: Process follow/unfollow action
      result <- if (isFollow) handleFollow(userID, followeeID) else handleUnfollow(userID, followeeID)

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