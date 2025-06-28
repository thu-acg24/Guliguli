package Impl


import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import com.typesafe.scalalogging.Logger
import org.joda.time.DateTime
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import cats.implicits._
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
import io.circe._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ChangeFavoriteMessagePlanner(
                                         token: String,
                                         videoID: Int,
                                         isFav: Boolean,
                                         override val planContext: PlanContext
                                       ) extends Planner[Option[String]] {
  private val logger = Logger(getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Validate Token
      userIDOpt <- validateToken()
      _ <- IO(logger.info(s"UserIDOpt: ${userIDOpt}"))

      // Step 2: Handle Token validation result
      result <- userIDOpt match {
        case None =>
          IO(Some("Invalid Token")) // If token is invalid, return error
        case Some(userID) =>
          for {
            // Step 2: Validate videoID existence and status
            videoStatusOpt <- validateVideoStatus(videoID)
            _ <- IO(logger.info(s"VideoStatusOpt: ${videoStatusOpt}"))

            // Step 3: Proceed based on video validation result
            result <- videoStatusOpt match {
              case None =>
                IO(Some("Video Not Found")) // If video is not valid, return error
              case Some(_) =>
                // Step 4: Update Favorite Record
                updateFavoriteRecord(userID, videoID, isFav)
            }
          } yield result
      }
    } yield result
  }

  private def validateToken()(using PlanContext): IO[Option[Int]] = {
    logger.info("[validateToken] Validating token")
    getUIDByTokenMessage(token).send
  }

  private def validateVideoStatus(videoID: Int)(using PlanContext): IO[Option[Json]] = {
    logger.info("[validateVideoStatus] Validating videoID existence and status")
    val sql = s"SELECT status FROM ${schemaName}.video_table WHERE video_id = ?;"
    readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
      case Some(json) =>
        val status = decodeField[String](json, "status")
        if (status == "Public") Some(json) else None
      case None => None
    }
  }

  private def updateFavoriteRecord(userID: Int, videoID: Int, isFav: Boolean)(using PlanContext): IO[Option[String]] = {
    if (isFav) {
      addFavoriteRecord(userID, videoID)
    } else {
      removeFavoriteRecord(userID, videoID)
    }
  }

  private def addFavoriteRecord(userID: Int, videoID: Int)(using PlanContext): IO[Option[String]] = {
    logger.info(s"[addFavoriteRecord] Adding favorite record for userID=${userID}, videoID=${videoID}")
    val checkSql = s"SELECT * FROM ${schemaName}.favorite_record_table WHERE user_id = ? AND video_id = ?;"
    readDBJsonOptional(checkSql, List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString)
    )).flatMap {
      case Some(_) => IO(Some("Already Favorited"))
      case None =>
        val insertSql = s"INSERT INTO ${schemaName}.favorite_record_table (user_id, video_id, timestamp) VALUES (?, ?, ?);"
        val timestamp = DateTime.now().getMillis.toString
        writeDB(insertSql, List(
          SqlParameter("Int", userID.toString),
          SqlParameter("Int", videoID.toString),
          SqlParameter("DateTime", timestamp)
        )).flatMap { _ =>
          val updateSql = s"UPDATE ${schemaName}.video_table SET favorites = favorites + 1 WHERE video_id = ?;"
          writeDB(updateSql, List(SqlParameter("Int", videoID.toString))).map(_ => None)
        }
    }
  }

  private def removeFavoriteRecord(userID: Int, videoID: Int)(using PlanContext): IO[Option[String]] = {
    logger.info(s"[removeFavoriteRecord] Removing favorite record for userID=${userID}, videoID=${videoID}")
    val checkSql = s"SELECT * FROM ${schemaName}.favorite_record_table WHERE user_id = ? AND video_id = ?;"
    readDBJsonOptional(checkSql, List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString)
    )).flatMap {
      case None => IO(Some("Not Favorited Yet"))
      case Some(_) =>
        val deleteSql = s"DELETE FROM ${schemaName}.favorite_record_table WHERE user_id = ? AND video_id = ?;"
        writeDB(deleteSql, List(
          SqlParameter("Int", userID.toString),
          SqlParameter("Int", videoID.toString)
        )).flatMap { _ =>
          val updateSql = s"UPDATE ${schemaName}.video_table SET favorites = favorites - 1 WHERE video_id = ?;"
          writeDB(updateSql, List(SqlParameter("Int", videoID.toString))).map(_ => None)
        }
    }
  }
}