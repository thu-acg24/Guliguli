package Impl


import APIs.UserService.GetUIDByTokenMessage
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
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ChangeLikeMessagePlanner(
                                     token: String,
                                     videoID: Int,
                                     isLike: Boolean,
                                     override val planContext: PlanContext
                                   ) extends Planner[Option[String]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Validate the token and get user ID
      _ <- IO(logger.info(s"[Step 1] Validating token: $token"))
      userIDOpt <- GetUIDByTokenMessage(token).send
      result <- userIDOpt match {
        case None =>
          // Token is invalid
          IO(logger.error("[Step 1.1] Invalid Token")) >>
          IO.pure(Some("Invalid Token"))

        case Some(userID) =>
          IO(logger.info(f"[Step 1.2] Valid token. Retrieved userID=$userID")) >>
          validateAndProcess(userID)
      }
    } yield result
  }

  private def validateAndProcess(userID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      // Step 2: Validate Video ID
      _ <- IO(logger.info(s"[Step 2] Validating videoID: $videoID"))
      videoStatusOpt <- getVideoStatus(videoID)
      result <- videoStatusOpt match {
        case None =>
          // Video does not exist or is not public
          IO(logger.error("[Step 2.1] Video Not Found")) >>
          IO.pure(Some("Video Not Found"))

        case Some("Public") =>
          // Video is valid, process like or dislike
          IO(logger.info("[Step 2.2] Video is public. Proceeding to Like/Dislike operation")) >>
          processLikeOrDislike(userID)

        case Some(otherStatus) =>
          // Video status is not valid for processing
          IO(logger.error(s"[Step 2.3] Invalid Video Status: $otherStatus")) >>
          IO.pure(Some("Video Not Found"))
      }
    } yield result
  }

  private def getVideoStatus(videoID: Int)(using PlanContext): IO[Option[String]] = {
    val sql = s"SELECT status FROM ${schemaName}.video_table WHERE video_id = ?"
    val params = List(SqlParameter("Int", videoID.toString))
    readDBJsonOptional(sql, params).map(_.map(decodeField[String](_, "status")))
  }

  private def processLikeOrDislike(userID: Int)(using PlanContext): IO[Option[String]] = {
    if (isLike) {
      likeAction(userID, videoID)
    } else {
      dislikeAction(userID, videoID)
    }
  }

  private def likeAction(userID: Int, videoID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      // Check if the like record already exists
      _ <- IO(logger.info(s"[Step 3.1.1] Checking existing like record for userID=$userID, videoID=$videoID"))
      existingLike <- checkLikeRecord(userID, videoID)
      result <- if (existingLike) {
        IO(logger.info("[Step 3.1.2] Already Liked")) >>
        IO.pure(Some("Already Liked"))
      } else {
        for {
          _ <- IO(logger.info("[Step 3.1.3] Inserting new like record"))
          _ <- insertLikeRecord(userID, videoID)
          _ <- IO(logger.info("[Step 3.1.4] Incrementing like count in VideoTable"))
          _ <- updateVideoLikeCount(videoID, increment = true)
        } yield None
      }
    } yield result
  }

  private def dislikeAction(userID: Int, videoID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      // Check if the like record exists
      _ <- IO(logger.info(s"[Step 3.2.1] Checking existing like record for userID=$userID, videoID=$videoID"))
      existingLike <- checkLikeRecord(userID, videoID)
      result <- if (!existingLike) {
        IO(logger.info("[Step 3.2.2] Not Liked Yet")) >>
        IO.pure(Some("Not Liked Yet"))
      } else {
        for {
          _ <- IO(logger.info("[Step 3.2.3] Deleting like record"))
          _ <- deleteLikeRecord(userID, videoID)
          _ <- IO(logger.info("[Step 3.2.4] Decrementing like count in VideoTable"))
          _ <- updateVideoLikeCount(videoID, increment = false)
        } yield None
      }
    } yield result
  }

  private def checkLikeRecord(userID: Int, videoID: Int)(using PlanContext): IO[Boolean] = {
    val sql = s"SELECT 1 FROM ${schemaName}.like_record_table WHERE user_id = ? AND video_id = ? LIMIT 1"
    val params = List(SqlParameter("Int", userID.toString), SqlParameter("Int", videoID.toString))
    readDBJsonOptional(sql, params).map(_.isDefined)
  }

  private def insertLikeRecord(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    val sql = s"INSERT INTO ${schemaName}.like_record_table (user_id, video_id, timestamp) VALUES (?, ?, ?)"
    writeDB(sql, List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString),
      SqlParameter("DateTime", DateTime.now.getMillis.toString)
    )).void
  }

  private def deleteLikeRecord(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    val sql = s"DELETE FROM ${schemaName}.like_record_table WHERE user_id = ? AND video_id = ?"
    writeDB(sql, List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString)
    )).void
  }

  private def updateVideoLikeCount(videoID: Int, increment: Boolean)(using PlanContext): IO[Unit] = {
    val operator = if (increment) "+" else "-"
    val sql = s"UPDATE ${schemaName}.video_table SET likes = likes $operator 1 WHERE video_id = ?"
    writeDB(sql, List(SqlParameter("Int", videoID.toString))).void
  }
}