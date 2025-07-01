package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class AddBrowseHistoryMessagePlanner(
                                           token: String,
                                           videoID: Int,
                                           override val planContext: PlanContext
                                         ) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info("Step 1: Validate user token and retrieve user ID"))
      userId<- getUserIdByToken()
      _ <-IO(logger.info(s"Validated user token, userId: ${userId}"))
      _ <-IO(logger.info("Step 2: Validate videoID and retrieve video information"))
      video<-getVideoInfo(videoID)
      _ <-IO(logger.info(s"Video validated successfully: videoID=${video.videoID}, title=${video.title}"))
      _ <-IO(logger.info("Step 3: Add browse history to the database"))
      _ <-addOrUpdateHistory(userId, video.videoID).map(_ => ())
    } yield ()
  }

  private def getUserIdByToken()(using PlanContext): IO[Int] = {
    GetUIDByTokenMessage(token).send.flatTap { userId =>
      IO{logger.info(s"Retrieved user ID: ${userId} for token: ${token}")}
      }
  }

  private def getVideoInfo(videoID: Int)(using PlanContext): IO[Video] = {
    QueryVideoInfoMessage(None, videoID).send.flatTap { video =>
      IO{logger.info(s"Retrieved video info: ${video}")}
      }
  }

  private def addOrUpdateHistory(userId: Int, videoId: Int)(using PlanContext): IO[Unit] = {
    for {
      timestamp <- IO(DateTime.now())
      _ <- IO(logger.info(s"Step 3.1: Check if user ${userId} has already viewed video ${videoId}"))
      exists <- checkExistingHistory(userId, videoId)
      _ <- if(exists){
        IO(logger.info(s"User ${userId} has viewed video ${videoId} before, updating timestamp")) *>
          updateHistoryTimestamp(userId, videoId, timestamp)
      } else {
           IO(logger.info(s"User ${userId} has not viewed video ${videoId} before, inserting new history record")) *>
             insertNewHistoryRecord(userId, videoId, timestamp)
      }
    } yield ()
  }

  private def checkExistingHistory(userId: Int, videoId: Int)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         |SELECT 1
         |FROM ${schemaName}.history_record_table
         |WHERE user_id = ? AND video_id = ?;
         """.stripMargin
    readDBJsonOptional(sql, List(
      SqlParameter("Int", userId.toString),
      SqlParameter("Int", videoId.toString)
    )).map(_.isDefined)
  }

  private def updateHistoryTimestamp(userId: Int, videoId: Int, timestamp: DateTime)(using PlanContext): IO[Unit] = {
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
    )).as(())
  }

  private def insertNewHistoryRecord(userId: Int, videoId: Int, timestamp: DateTime)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
         |INSERT INTO ${schemaName}.history_record_table (user_id, video_id, timestamp)
         |VALUES (?, ?, ?);
         """.stripMargin
    writeDB(sql, List(
      SqlParameter("Int", userId.toString),
      SqlParameter("Int", videoId.toString),
      SqlParameter("DateTime", timestamp.getMillis.toString)
    )).as(())
  }
}