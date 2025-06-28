package Impl


import Objects.VideoService.VideoStatus
import Objects.VideoService.Video
import APIs.VideoService.QueryVideoInfoMessage
import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
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
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class AddBrowseHistoryMessagePlanner(
                                           token: String,
                                           videoID: Int,
                                           override val planContext: PlanContext
                                         ) extends Planner[Option[String]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info("Step 1: Validate user token and retrieve user ID"))
      userIdOption <- getUserIdByToken()
      result <- userIdOption match {
        case None =>
          val errorMsg = "Invalid Token"
          IO(logger.info(s"Token validation failed: ${errorMsg}")) *> IO(Some(errorMsg))
        case Some(userId) =>
          _ <- IO(logger.info(s"Validated user token, userId: ${userId}"))
          IO(logger.info("Step 2: Validate videoID and retrieve video information")) *>
          getVideoInfo(videoID).flatMap {
            case None =>
              val errorMsg = "Invalid video ID"
              IO(logger.info(s"Video validation failed: ${errorMsg}")) *> IO(Some(errorMsg))
            case Some(video) =>
              IO(logger.info(s"Video validated successfully: videoID=${video.videoID}, title=${video.title}")) *>
              IO(logger.info("Step 3: Add browse history to the database")) *>
              addOrUpdateHistory(userId, video.videoID).map(_ => None)
          }
      }
    } yield result
  }

  private def getUserIdByToken()(using PlanContext): IO[Option[Int]] = {
    getUIDByTokenMessage(token).send.tapWith { userIdOption =>
      IO {
        userIdOption match {
          case Some(userId) => logger.info(s"Retrieved user ID: ${userId} for token: ${token}")
          case None         => logger.info(s"Failed to retrieve user ID for token: ${token}")
        }
      }
    }
  }

  private def getVideoInfo(videoID: Int)(using PlanContext): IO[Option[Video]] = {
    QueryVideoInfoMessage(None, videoID).send.tapWith { videoOption =>
      IO {
        videoOption match {
          case Some(video) => logger.info(s"Retrieved video info: ${video}")
          case None        => logger.info(s"No video found with ID: ${videoID}")
        }
      }
    }
  }

  private def addOrUpdateHistory(userId: Int, videoId: Int)(using PlanContext): IO[Unit] = {
    val timestamp = DateTime.now()
    for {
      _ <- IO(logger.info(s"Step 3.1: Check if user ${userId} has already viewed video ${videoId}"))
      existingHistoryOption <- checkExistingHistory(userId, videoId)
      _ <- existingHistoryOption match {
        case Some(_) =>
          IO(logger.info(s"User ${userId} has viewed video ${videoId} before, updating timestamp")) *>
            updateHistoryTimestamp(userId, videoId, timestamp)
        case None =>
          IO(logger.info(s"User ${userId} has not viewed video ${videoId} before, inserting new history record")) *>
            insertNewHistoryRecord(userId, videoId, timestamp)
      }
    } yield ()
  }

  private def checkExistingHistory(userId: Int, videoId: Int)(using PlanContext): IO[Option[Unit]] = {
    val sql =
      s"""
         |SELECT 1
         |FROM ${schemaName}.history_record_table
         |WHERE user_id = ? AND video_id = ?;
         """.stripMargin
    readDBJsonOptional(sql, List(
      SqlParameter("Int", userId.toString),
      SqlParameter("Int", videoId.toString)
    )).map(_.map(_ => ()))
  }

  private def updateHistoryTimestamp(userId: Int, videoId: Int, timestamp: DateTime)(using PlanContext): IO[String] = {
    val sql =
      s"""
         |UPDATE ${schemaName}.history_record_table
         |SET timestamp = ?
         |WHERE user_id = ? AND video_id = ?;
         """.stripMargin
    writeDB(sql, List(
      SqlParameter("DateTime", timestamp.getMillis.toString),
      SqlParameter("Int", userId.toString),
      SqlParameter("Int", videoId.toString)
    ))
  }

  private def insertNewHistoryRecord(userId: Int, videoId: Int, timestamp: DateTime)(using PlanContext): IO[String] = {
    val sql =
      s"""
         |INSERT INTO ${schemaName}.history_record_table (user_id, video_id, timestamp)
         |VALUES (?, ?, ?);
         """.stripMargin
    writeDB(sql, List(
      SqlParameter("Int", userId.toString),
      SqlParameter("Int", videoId.toString),
      SqlParameter("DateTime", timestamp.getMillis.toString)
    ))
  }
}