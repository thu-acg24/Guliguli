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
      userID<- getUserIDByToken()
      _ <-IO(logger.info(s"Validated user token, userID: $userID"))
      _ <-IO(logger.info("Step 2: Validate videoID and retrieve video information"))
      video<-getVideoInfo(videoID)
      _ <-IO(logger.info(s"Video validated successfully: videoID=${video.videoID}, title=${video.title}"))
      _ <-IO(logger.info("Step 3: Add browse history to the database"))
      _ <-addOrUpdateHistory(userID, video.videoID).map(_ => ())
    } yield ()
  }

  private def getUserIDByToken()(using PlanContext): IO[Int] = {
    GetUIDByTokenMessage(token).send.flatTap { userID =>
      IO{logger.info(s"Retrieved user ID: $userID for token: $token")}
      }
  }

  private def getVideoInfo(videoID: Int)(using PlanContext): IO[Video] = {
    QueryVideoInfoMessage(Some(token), videoID).send.flatTap { video =>
      IO{logger.info(s"Retrieved video info: $video")}
      }
  }

  private def addOrUpdateHistory(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    for {
      timestamp <- IO(DateTime.now())
      _ <- IO(logger.info(s"Step 3.1: Check if user $userID has already viewed video $videoID"))
      exists <- checkExistingHistory(userID, videoID)
      _ <- if(exists){
        IO(logger.info(s"User $userID has viewed video $videoID before, updating timestamp")) *>
          updateHistoryTimestamp(userID, videoID, timestamp)
      } else {
           IO(logger.info(s"User $userID has not viewed video $videoID before, inserting new history record")) *>
             insertNewHistoryRecord(userID, videoID, timestamp)
      }
    } yield ()
  }

  private def checkExistingHistory(userID: Int, videoID: Int)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         |SELECT 1
         |FROM $schemaName.history_record_table
         |WHERE user_id = ? AND video_id = ?;
         """.stripMargin
    readDBJsonOptional(sql, List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString)
    )).map(_.isDefined)
  }

  private def updateHistoryTimestamp(userID: Int, videoID: Int, timestamp: DateTime)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
         |UPDATE $schemaName.history_record_table
         |SET timestamp = ?
         |WHERE user_id = ? AND video_id = ?;
         """.stripMargin
    writeDB(sql, List(
      SqlParameter("DateTime", timestamp.getMillis.toString),
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString)
    )).as(())
  }

  private def insertNewHistoryRecord(userID: Int, videoID: Int, timestamp: DateTime)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
         |INSERT INTO $schemaName.history_record_table (user_id, video_id, view_time)
         |VALUES (?, ?, ?);
         """.stripMargin
    writeDB(sql, List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString),
      SqlParameter("DateTime", timestamp.getMillis.toString)
    )).as(())
  }
}