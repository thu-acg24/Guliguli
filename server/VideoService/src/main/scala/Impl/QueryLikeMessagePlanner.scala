package Impl

import APIs.UserService.GetUIDByTokenMessage
import Common.APIException.InvalidInputException
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.slf4j.LoggerFactory

case class QueryLikeMessagePlanner(
                                        token: String,
                                        videoID: Int,
                                        override val planContext: PlanContext
                                      ) extends Planner[Boolean] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Boolean] = {
    for {
      // Step 1: Validate Token
      userID <- validateToken()
      _ <- IO(logger.info(s"UserID: $userID"))

      // Step 2: Validate videoID existence
      _ <- validateVideoExistence(videoID)
      
      // Step 3: Check if user likes this video
      isLiked <- checkLikeStatus(userID, videoID)
      _ <- IO(logger.info(s"[QueryLike] UserID $userID likes videoID $videoID: $isLiked"))
    } yield isLiked
  }

  private def validateToken()(using PlanContext): IO[Int] = {
    IO(logger.info("[validateToken] Validating token")) >>
    GetUIDByTokenMessage(token).send
  }

  private def validateVideoExistence(videoID: Int)(using PlanContext): IO[Unit] = {
    IO(logger.info("[validateVideoExistence] Validating videoID existence")) >>
    {
      val sql = s"SELECT video_id FROM $schemaName.video_table WHERE video_id = ? AND status = 'Approved';"
      readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
        case Some(_) => ()
        case None => throw InvalidInputException("找不到视频")
      }
    }
  }

  private def checkLikeStatus(userID: Int, videoID: Int)(using PlanContext): IO[Boolean] = {
    val sql = s"SELECT * FROM $schemaName.like_record_table WHERE user_id = ? AND video_id = ?;"
    readDBJsonOptional(sql, List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString)
    )).map(_.isDefined)
  }
}
