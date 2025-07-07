package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.RecommendationService.UpdateFeedbackLikeMessage
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
                                   ) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate Token
      userID <- validateToken()
      _ <- IO(logger.info(s"UserID: $userID"))

      // Step 2: Validate videoID existence and status
      videoStatus <- validateVideoStatus(videoID)
      _ <- IO(logger.info(s"VideoStatus: $videoStatus"))

      // Step 3: Update like record
      result <- updateLikeRecord(userID, videoID, isLike)
      
      // Step 4: Notify RecommendationService
      _ <- notifyRecommendationService(userID, videoID, isLike)
    } yield result
  }

  private def validateToken()(using PlanContext): IO[Int] = {
    IO(logger.info("[validateToken] Validating token")) >>
    GetUIDByTokenMessage(token).send
  }

  private def validateVideoStatus(videoID: Int)(using PlanContext): IO[Json] = {
    IO(logger.info("[validateVideoStatus] Validating videoID existence and status")) >>
    {
      val sql = s"SELECT status FROM $schemaName.video_table WHERE video_id = ?;"
      readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
        case Some(json) =>
          val status = decodeField[String](json, "status")
          if (status == "Approved") json else throw InvalidInputException("找不到视频")
        case None => throw InvalidInputException("找不到视频")
      }
    }
  }

  private def updateLikeRecord(userID: Int, videoID: Int, isLike: Boolean)(using PlanContext): IO[Unit] = {
    if (isLike) {
      addLikeRecord(userID, videoID)
    } else {
      removeLikeRecord(userID, videoID)
    }
  }

  private def addLikeRecord(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    IO(logger.info(s"[addLikeRecord] Adding like record for userID=$userID, videoID=$videoID")) >> {
      val checkSql = s"SELECT * FROM $schemaName.like_record_table WHERE user_id = ? AND video_id = ?;"
      readDBJsonOptional(checkSql, List(
        SqlParameter("Int", userID.toString),
        SqlParameter("Int", videoID.toString)
      )).flatMap {
        case Some(_) => IO(logger.info("Already Liked")) >> IO.unit
        case None =>
          val insertSql = s"INSERT INTO $schemaName.like_record_table (user_id, video_id, timestamp) VALUES (?, ?, ?);"
          val updateSql = s"UPDATE $schemaName.video_table SET likes = likes + 1 WHERE video_id = ?;"
          for {
            timestamp <- IO(DateTime.now().getMillis.toString)
            _ <- writeDB(insertSql, List(
                SqlParameter("Int", userID.toString),
                SqlParameter("Int", videoID.toString),
                SqlParameter("DateTime", timestamp)
              ))
            _ <- writeDB(updateSql, List(SqlParameter("Int", videoID.toString)))
          } yield ()
      }
    }
  }

  private def removeLikeRecord(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    IO(logger.info(s"[removeLikeRecord] Removing like record for userID=$userID, videoID=$videoID")) >> {
      val checkSql = s"SELECT * FROM $schemaName.like_record_table WHERE user_id = ? AND video_id = ?;"
      readDBJsonOptional(checkSql, List(
        SqlParameter("Int", userID.toString),
        SqlParameter("Int", videoID.toString)
      )).flatMap {
        case None => IO(logger.info("Not Liked Yet")) >> IO.unit
        case Some(_) =>
          val deleteSql = s"DELETE FROM $schemaName.like_record_table WHERE user_id = ? AND video_id = ?;"
          val updateSql = s"UPDATE $schemaName.video_table SET likes = likes - 1 WHERE video_id = ?;"
          for {
            _ <- writeDB(deleteSql, List(
              SqlParameter("Int", userID.toString),
              SqlParameter("Int", videoID.toString)
            ))
            _ <- writeDB(updateSql, List(SqlParameter("Int", videoID.toString)))
          } yield ()
      }
    }
  }

  private def notifyRecommendationService(userID: Int, videoID: Int, isLike: Boolean)(using PlanContext): IO[Unit] = {
    IO(logger.info(s"[notifyRecommendationService] Notifying RecommendationService about like change: userID=$userID, videoID=$videoID, isLike=$isLike")) >> {
      UpdateFeedbackLikeMessage(token, videoID, isLike).send.handleErrorWith { error =>
        IO(logger.warn(s"Failed to notify RecommendationService: ${error.getMessage}")) >> IO.unit
      }
    }
  }
}