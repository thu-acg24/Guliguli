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

case class ChangeFavoriteMessagePlanner(
                                         token: String,
                                         videoID: Int,
                                         isFav: Boolean,
                                         override val planContext: PlanContext
                                       ) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate Token
      userID <- validateToken()
      _ <- IO(logger.info(s"UserID: ${userID}"))

      // Step 2: Validate videoID existence and status
      videoStatus <- validateVideoStatus(videoID)
      _ <- IO(logger.info(s"VideoStatus: ${videoStatus}"))

      // Step 3: Proceed based on video validation result
      result <- updateFavoriteRecord(userID, videoID, isFav)
    } yield result
  }

  private def validateToken()(using PlanContext): IO[Int] = {
    IO(logger.info("[validateToken] Validating token")) >>
    GetUIDByTokenMessage(token).send
  }

  private def validateVideoStatus(videoID: Int)(using PlanContext): IO[Json] = {
    IO(logger.info("[validateVideoStatus] Validating videoID existence and status")) >>
    {
      val sql = s"SELECT status FROM ${schemaName}.video_table WHERE video_id = ?;"
      readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
        case Some(json) =>
          val status = decodeField[String](json, "status")
          if (status == "Approved") json else throw InvalidInputException("找不到视频")
        case None => throw InvalidInputException("找不到视频")
      }
    }
  }

  private def updateFavoriteRecord(userID: Int, videoID: Int, isFav: Boolean)(using PlanContext): IO[Unit] = {
    if (isFav) {
      addFavoriteRecord(userID, videoID)
    } else {
      removeFavoriteRecord(userID, videoID)
    }
  }

  private def addFavoriteRecord(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    IO(logger.info(s"[addFavoriteRecord] Adding favorite record for userID=${userID}, videoID=${videoID}")) >> {
      val checkSql = s"SELECT * FROM ${schemaName}.favorite_record_table WHERE user_id = ? AND video_id = ?;"
      readDBJsonOptional(checkSql, List(
        SqlParameter("Int", userID.toString),
        SqlParameter("Int", videoID.toString)
      )).flatMap {
        case Some(_) => IO(logger.info("Already Favorited")) >> IO.unit
        case None =>
          val insertSql = s"INSERT INTO ${schemaName}.favorite_record_table (user_id, video_id, timestamp) VALUES (?, ?, ?);"
          val updateSql = s"UPDATE ${schemaName}.video_table SET favorites = favorites + 1 WHERE video_id = ?;"
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

  private def removeFavoriteRecord(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    IO(logger.info(s"[removeFavoriteRecord] Removing favorite record for userID=${userID}, videoID=${videoID}")) >> {
      val checkSql = s"SELECT * FROM ${schemaName}.favorite_record_table WHERE user_id = ? AND video_id = ?;"
      readDBJsonOptional(checkSql, List(
        SqlParameter("Int", userID.toString),
        SqlParameter("Int", videoID.toString)
      )).flatMap {
        case None => IO(logger.info("Not Favorited Yet")) >> IO.unit
        case Some(_) =>
          val deleteSql = s"DELETE FROM ${schemaName}.favorite_record_table WHERE user_id = ? AND video_id = ?;"
          val updateSql = s"UPDATE ${schemaName}.video_table SET favorites = favorites - 1 WHERE video_id = ?;"
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
}