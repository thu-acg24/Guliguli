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

case class RecordWatchDataMessagePlanner(
  token: String,
  videoID: Int,
  watchDuration: Float,
  override val planContext: PlanContext
) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate token and get user ID
      userID <- getUserIDByToken()
      _ <- validateVideo(userID)
      _ <- recordWatchData(userID, videoID)
    } yield ()
  }

  private def getUserIDByToken()(using PlanContext): IO[Int] = {
    logger.info(s"开始调用getUIDByTokenMessage解析Token: $token")
    GetUIDByTokenMessage(token).send
  }

  private def validateVideo(userID: Int)(using PlanContext): IO[Video] = {
    logger.info(s"开始获取视频: $videoID")
    QueryVideoInfoMessage(Some(token), videoID).send
  }

  private def recordWatchData(userID: Int, videoID: Int)(using PlanContext): IO[String] = {
    logger.info(s"开始记录观看数据: userID=$userID, videoID=$videoID, watchDuration=$watchDuration")
    val sql =
      s"""
      INSERT INTO ${schemaName}.watch_detail_table
      (user_id, video_id, watch_duration, created_at)
      VALUES (?, ?, ?, ?)
      """
    for {
      createdAt <- IO(DateTime.now().getMillis.toString)
      parameters = List(
        SqlParameter("Int", userID.toString),
        SqlParameter("Int", videoID.toString),
        SqlParameter("Float", watchDuration.toString),
        SqlParameter("DateTime", createdAt)
      )
      result <- writeDB(sql, parameters)
    } yield result
  }
}