package Impl

import APIs.UserService.GetUIDByTokenMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.VideoAuth.validateVideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
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
      _ <- validateVideoStatus(videoID)
      
      // Step 3: Check if user likes this video
      isLiked <- checkLikeStatus(userID, videoID)
      _ <- IO(logger.info(s"[QueryLike] UserID $userID likes videoID $videoID: $isLiked"))
    } yield isLiked
  }

  private def validateToken()(using PlanContext): IO[Int] = {
    IO(logger.info("[validateToken] Validating token")) >>
    GetUIDByTokenMessage(token).send
  }

  private def checkLikeStatus(userID: Int, videoID: Int)(using PlanContext): IO[Boolean] = {
    val sql = s"SELECT * FROM $schemaName.like_record_table WHERE user_id = ? AND video_id = ?;"
    readDBJsonOptional(sql, List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString)
    )).map(_.isDefined)
  }
}
