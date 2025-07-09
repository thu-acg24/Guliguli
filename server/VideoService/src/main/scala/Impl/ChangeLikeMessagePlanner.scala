package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.RecommendationService.UpdateFeedbackLikeMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.VideoAuth.validateVideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
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
            _ <- writeDB(insertSql + updateSql, List(
              SqlParameter("Int", userID.toString),
              SqlParameter("Int", videoID.toString),
              SqlParameter("DateTime", timestamp),
              SqlParameter("Int", videoID.toString)
            ))
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
            _ <- writeDB(deleteSql + updateSql, List(
              SqlParameter("Int", userID.toString),
              SqlParameter("Int", videoID.toString),
              SqlParameter("Int", videoID.toString)
            ))
          } yield ()
      }
    }
  }

  private def notifyRecommendationService(userID: Int, videoID: Int, isLike: Boolean)(using PlanContext): IO[Unit] = {
    IO(logger.info(s"[notifyRecommendationService] Notifying about like change: userID=$userID, videoID=$videoID, isLike=$isLike")) >>
      UpdateFeedbackLikeMessage(token, videoID, isLike).send
  }
}