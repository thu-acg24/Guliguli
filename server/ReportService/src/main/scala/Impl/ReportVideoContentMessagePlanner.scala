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
) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      // Step 1: 校验 token 是否有效并获取 userID
      _ <- IO(logger.info(s"Validating token: ${token}"))
      userID <- GetUIDByTokenMessage(token).send
      // Step 2: 检测弹幕是否存在
      _ <- QueryVideoInfoMessage(Some(token), videoID).send
      // Step 3: 检查重复举报
      _ <- IO(logger.info(s"Checking for duplicate pending reports for videoID: ${videoID} and userID: ${userID}"))
      alreadyExists <- checkDuplicateReport(videoID, userID)
      _ <- if alreadyExists then IO.raiseError(RuntimeException("已经举报过该视频")) else IO.unit
      // Step 4: 插入举报记录
      _ <- IO(logger.info(s"Inserting new report for videoID: ${videoID}, userID: ${userID}, reason: ${reason}"))
      _ <- insertReportRecord(userID, videoID, reason)
    } yield ()
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

  private def insertReportRecord(userID: Int, videoID: Int, reason: String)(using PlanContext): IO[String] = {
    val sql =
      s"""
         INSERT INTO ${schemaName}.report_video_table
         (video_id, reporter_id, reason, status, timestamp)
         VALUES (?, ?, ?, 'Pending', ?)
       """
    IO(DateTime.now().getMillis.toString).flatMap { timestamp =>
      writeDB(sql, List(
        SqlParameter("Int", videoID.toString),
        SqlParameter("Int", userID.toString),
        SqlParameter("String", reason),
        SqlParameter("DateTime", timestamp)
      ))
    }
  }
}