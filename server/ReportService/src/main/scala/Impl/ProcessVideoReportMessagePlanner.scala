package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.ReportService.ReportStatus
import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserRoleMessage
import APIs.VideoService.QueryVideoInfoMessage
import APIs.VideoService.ChangeVideoStatusMessage
import Objects.VideoService.VideoStatus
import Utils.NotifyProcess.sendNotification
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Objects.MessageService.Message
import Objects.VideoService.Video
import Objects.UserService.UserRole
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
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ProcessVideoReportMessagePlanner(
    token: String,
    reportID: Int,
    status: ReportStatus,
    override val planContext: PlanContext
) extends Planner[String] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"Start processing report with ID: ${reportID}, status: ${status}"))
      // Step 1: 校验用户权限
      userID <- getUserIDByToken()
      userRole <- getUserRoleByToken()
      _ <- checkUserRole(userRole)

      // Step 2: 校验 reportID 是否存在
      reportDetails <- validateReportID()
      videoID = reportDetails._1
      reporterID = reportDetails._2

      // Step 3: 检查视频是否存在、且是公开状态
      videoTitle <- validateVideo(videoID)

      // Step 4: 如果需要删除视频
      _ <- privatizeVideoIfNeeded(videoID)
      _ <- sendNotificationToPublisher(publisherID, videoTitle)

      // Step 5: 更新举报记录状态
      _ <- updateReportStatus()

      // Step 6: 向用户发送通知
      _ <- sendNotificationToReporter(reporterID, videoTitle)

      _ <- IO(logger.info("Process completed successfully"))
    } yield "Operation completed successfully"
  }

  private def getUserIDByToken()(using PlanContext): IO[Int] = {
    logger.info("Fetching user ID by token")
    GetUIDByTokenMessage(token).send.flatMap {
      case Some(userID) => IO.pure(userID)
      case None =>
        IO(logger.error("Token verification failed")) *>
        IO.raiseError(new Exception("Unauthorized Access"))
    }
  }

  private def getUserRoleByToken()(using PlanContext): IO[UserRole] = {
    logger.info("Fetching user role by token")
    QueryUserRoleMessage(token).send.flatMap {
      case Some(role) => IO.pure(role)
      case None =>
        IO(logger.error("Failed to fetch user role")) *>
        IO.raiseError(new Exception("Unauthorized Access"))
    }
  }

  private def checkUserRole(userRole: UserRole)(using PlanContext): IO[Unit] = {
    if (userRole == UserRole.Auditor) IO.unit
    else {
      IO(logger.error("Unauthorized access, user is not an Auditor")) *>
      IO.raiseError(new Exception("Only auditors can perform this operation"))
    }
  }

  private def validateReportID()(using PlanContext): IO[(Int, Int)] = {
    logger.info(s"Validating report ID: ${reportID}")
    val sql = s"""
      SELECT video_id, reporter_id, status FROM ${schemaName}.report_video_table WHERE report_id = ?;
      """
    readDBJsonOptional(sql, List(SqlParameter("Int", reportID.toString))).flatMap {
      case Some(json) =>
        val videoID = decodeField[Int](json, "video_id")
        val reporterID = decodeField[Int](json, "reporter_id")
        val currentStatus = decodeField[String](json, "status")
        if (currentStatus == ReportStatus.Pending.toString) IO.pure((videoID, reporterID))
        else {
          IO(logger.error(s"Report not found or already processed: status=${currentStatus}")) *>
          IO.raiseError(new Exception("Report Not Found or Already Processed"))
        }
      case None =>
        IO(logger.error("Report ID not found")) *>
        IO.raiseError(new Exception("Report Not Found"))
    }
  }

  private def validateVideo(videoID: Int)(using PlanContext): IO[String] = {
    logger.info(s"Validating video with ID: ${videoID}")
    QueryVideoInfoMessage(None, videoID).send.flatMap {
      case Some(video) if video.status == VideoStatus.Approved =>
        IO.pure(video.title)
      case Some(_) =>
        IO(logger.error("Video is not public")) *>
        IO.raiseError(new Exception("Video is not public"))
      case None =>
        IO(logger.error("Video not found")) *>
        IO.raiseError(new Exception("Video not found"))
    }
  }

  private def privatizeVideoIfNeeded(videoID: Int)(using PlanContext): IO[Unit] = {
    if (status == ReportStatus.Resolved) {
      logger.info(s"Deleting video with ID: ${videoID}")
      ChangeVideoStatusMessage(token, videoID, VideoStatus.Rejected).send.flatMap {
        case Some(error) =>
          IO(logger.error(s"Failed to privatize video: ${error}")) *>
          IO.raiseError(new Exception("Failed to privatize video"))
        case None => IO.unit
      }
    } else IO.unit
  }

  private def updateReportStatus()(using PlanContext): IO[Unit] = {
    logger.info(s"Updating report status for ID: ${reportID} to ${status}")
    val sql = s"""
      UPDATE ${schemaName}.report_video_table
      SET status = ?
      WHERE report_id = ?;
      """
    writeDB(sql, List(
      SqlParameter("String", status.toString),
      SqlParameter("Int", reportID.toString)
    )).flatMap {
      case "Operation(s) done successfully" => IO.unit
      case _ =>
        IO(logger.error("Failed to update report status")) *>
        IO.raiseError(new Exception("Failed to process report"))
    }
  }

  private def sendNotificationToReporter(
      reporterID: Int,
      videoTitle: String
  )(using PlanContext): IO[Unit] = {
    val messageContent = s"Your report regarding video '${videoTitle}' has been processed with status: ${status}"
    logger.info(s"Sending notification to reporter: ${reporterID}")
    sendNotification(token, reporterID, messageContent).flatMap {
      case Some(error) =>
        IO(logger.error(s"Failed to send notification: ${error}")) *>
        IO.raiseError(new Exception("Failed to send notification"))
      case None => IO.unit
    }
  }
}