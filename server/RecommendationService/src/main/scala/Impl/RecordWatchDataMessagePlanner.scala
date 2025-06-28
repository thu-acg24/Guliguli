package Impl


import Objects.VideoService.VideoStatus
import Objects.VideoService.Video
import APIs.VideoService.QueryVideoInfoMessage
import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.VideoService.VideoStatus
import Objects.VideoService.Video
import APIs.VideoService.QueryVideoInfoMessage
import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class RecordWatchDataMessagePlanner(
  token: String,
  videoID: Int,
  watchDuration: Float,
  override val planContext: PlanContext
) extends Planner[Option[String]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Validate token and get user ID
      userIDOpt <- getUserIdByToken()
      _ <- IO(logger.info(s"Token校验结果: ${userIDOpt.map(_.toString).getOrElse("Invalid Token")}"))
      result <- userIDOpt match {
        case None =>
          IO(logger.info("Token无效，返回Invalid Token")) *> IO.pure(Some("Invalid Token"))
        case Some(userID) =>
          // Step 2: Check if video ID is valid
          validateVideo(userID)
      }
    } yield result
  }

  private def getUserIdByToken()(using PlanContext): IO[Option[Int]] = {
    logger.info(s"开始调用getUIDByTokenMessage解析Token: $token")
    getUIDByTokenMessage(token).send
  }

  private def validateVideo(userID: Int)(using PlanContext): IO[Option[String]] = {
    logger.info(s"开始检查视频ID是否有效: $videoID")
    QueryVideoInfoMessage(Some(token), videoID).send.flatMap {
      case None =>
        IO(logger.info("视频不存在或状态为删除，返回Video Not Found")) *> IO.pure(Some("Video Not Found"))
      case Some(video) if video.status == VideoStatus.Rejected || video.status == VideoStatus.Pending =>
        IO(logger.info(s"视频状态为${video.status}，返回Video Not Found")) *> IO.pure(Some("Video Not Found"))
      case Some(video) =>
        IO(logger.info(s"视频合法且存在: ${video.videoID}")) *> recordWatchData(userID, video.videoID)
    }
  }

  private def recordWatchData(userID: Int, videoID: Int)(using PlanContext): IO[Option[String]] = {
    logger.info(s"开始记录观看数据: userID=$userID, videoID=$videoID, watchDuration=$watchDuration")
    val sql =
      s"""
      INSERT INTO ${schemaName}.watch_detail_table
      (user_id, video_id, watch_duration, timestamp)
      VALUES (?, ?, ?, ?)
      """
    val parameters = List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", videoID.toString),
      SqlParameter("Float", watchDuration.toString),
      SqlParameter("DateTime", DateTime.now().getMillis.toString)
    )
    writeDB(sql, parameters).attempt.flatMap {
      case Right(_) =>
        IO(logger.info("观看记录插入成功")) *> IO.pure(None)
      case Left(exception) =>
        logger.error(s"观看记录插入失败，异常: ${exception.getMessage}")
        IO.pure(Some("Unable to record watch detail"))
    }
  }
}