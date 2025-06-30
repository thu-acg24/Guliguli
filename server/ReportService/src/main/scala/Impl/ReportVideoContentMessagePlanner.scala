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
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ReportVideoContentMessagePlanner(
    token: String,
    videoID: Int,
    reason: String,
    override val planContext: PlanContext
) extends Planner[Option[String]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Validate token and get userID
      _ <- IO(logger.info(s"[Step 1] 验证 token：${token}"))
      userIDOption <- validateToken()
      result <- userIDOption match {
        case None =>
          IO(logger.warn(s"[Step 1.1] 无效的 Token：${token}")) >>
            IO.pure(Some("Invalid Token"))
        case Some(userID) =>
          // Step 2: Check if video exists
          checkVideoAndProceed(userID)
      }
    } yield result
  }

  private def validateToken()(using PlanContext): IO[Option[Int]] = {
    GetUIDByTokenMessage(token).send
  }

  private def checkVideoAndProceed(userID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"[Step 2] 验证视频是否存在，videoID: ${videoID}"))
      videoOption <- QueryVideoInfoMessage(None, videoID).send
      result <- videoOption match {
        case None =>
          IO(logger.warn(s"[Step 2.1] 视频不存在，videoID: ${videoID}")) >>
            IO.pure(Some("Video Not Found or Unavailable"))
        case Some(video) if video.status != VideoStatus.Approved =>
          IO(logger.warn(s"[Step 2.2] 视频状态不可用，videoID: ${videoID}, 状态: ${video.status}")) >>
            IO.pure(Some("Video Not Found or Unavailable"))
        case Some(video) =>
          checkDuplicateAndProceed(userID, video)
      }
    } yield result
  }

  private def checkDuplicateAndProceed(userID: Int, video: Video)(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"[Step 3] 检查举报是否重复，userID: ${userID}, videoID: ${videoID}"))
      duplicateReportFound <- checkDuplicateReport(userID, videoID)
      result <- if (duplicateReportFound) {
        IO(logger.warn(s"[Step 3.1] 重复举报存在，userID: ${userID}, videoID: ${videoID}")) >>
          IO.pure(Some("Duplicate Pending Report Found"))
      } else {
        // Step 4: Insert a new report
        insertReportAndReturnResult(userID, videoID, reason)
      }
    } yield result
  }

  private def checkDuplicateReport(userID: Int, videoID: Int)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         SELECT COUNT(1)
         FROM ${schemaName}.report_video_table
         WHERE video_id = ? AND reporter_id = ? AND status = 'Pending';
       """
    readDBInt(
      sql,
      List(
        SqlParameter("Int", videoID.toString),
        SqlParameter("Int", userID.toString)
      )
    ).map(_ > 0)
  }

  private def insertReportAndReturnResult(userID: Int, videoID: Int, reason: String)(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"[Step 4] 插入新的举报记录，userID: ${userID}, videoID: ${videoID}, reason: ${reason}"))
      insertResult <- insertNewReport(userID, videoID, reason)
      result <- if (insertResult) {
        IO(logger.info(s"[Step 4.1] 举报记录成功保存，userID: ${userID}, videoID: ${videoID}")) >>
          IO.pure(None)
      } else {
        IO(logger.error(s"[Step 4.2] 举报记录保存失败，userID: ${userID}, videoID: ${videoID}")) >>
          IO.pure(Some("Failed to save report record"))
      }
    } yield result
  }

  private def insertNewReport(userID: Int, videoID: Int, reason: String)(using PlanContext): IO[Boolean] = {
    val timestamp = DateTime.now()
    val sql =
      s"""
         INSERT INTO ${schemaName}.report_video_table
         (video_id, reporter_id, reason, status, timestamp)
         VALUES (?, ?, ?, 'Pending', ?);
       """
    writeDB(
      sql,
      List(
        SqlParameter("Int", videoID.toString),
        SqlParameter("Int", userID.toString),
        SqlParameter("String", reason),
        SqlParameter("DateTime", timestamp.getMillis.toString)
      )
    ).map(_ == "Operation(s) done successfully")
  }
}