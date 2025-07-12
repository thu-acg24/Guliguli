package Impl


import APIs.MessageService.SendNotificationMessage
import Common.APIException.InvalidInputException
import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserRoleMessage
import APIs.VideoService.ChangeVideoStatusMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.MessageService.Message
import Objects.ReportService.ReportStatus
import Objects.UserService.UserRole
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import Utils.ValidateProcess.validateTokenAndRole
import cats.effect.IO
import cats.implicits.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ProcessVideoReportMessagePlanner(
    token: String,
    reportID: Int,
    status: ReportStatus,
    override val planContext: PlanContext
) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    if status == ReportStatus.Pending then return IO.raiseError(InvalidInputException("不允许改为等待状态"))
    if status == ReportStatus.Rejected then return for {
      _ <- IO(logger.info("开始处理 ProcessVideoReportMessage 请求"))
      // 特判被拒绝的举报
      _ <- validateTokenAndRole(token)
      (reporterID, videoID) <- validateReportID(reportID)
      _ <- updateReportStatus(reportID, status)
    } yield ()
    for {
      _ <- IO(logger.info("开始处理 ProcessVideoReportMessage 请求"))
      // Step 1: 校验 token 是否有效
      _ <- validateTokenAndRole(token)
      (reporterID, videoID) <- validateReportID(reportID)
      (videoTitle, uploaderID) <- validateVideo(videoID)
      _ <- updateReportStatus(reportID, status)
      _ <- ChangeVideoStatusMessage(token, videoID, VideoStatus.Rejected).send
      _ <- SendNotificationMessage(token, reporterID,
        s"举报处理通知",
        s"您举报的视频 $videoTitle 已被处理").send
      // _ <- SendNotificationMessage(token, uploaderID,
      //  s"视频违规通知",
      //  s"您的视频 $videoTitle 被举报并已被审核员下架").send
    } yield ()
  }

  private def validateReportID(
                                reportID: Int
                              )(using PlanContext): IO[(Int, Int)] = {
    for {
      _ <- IO(logger.info(s"校验 reportID [$reportID] 是否存在"))
      result <- readDBJsonOptional(
        s"SELECT reporter_id, video_id, status FROM $schemaName.report_video_table WHERE report_id = ?;",
        List(SqlParameter("Int", reportID.toString))
      )
    } yield result match {
      case None =>
        throw InvalidInputException("Report Not Found or Already Processed")
      case Some(json) =>
        val status = decodeField[String](json, "status")
        if (status != ReportStatus.Pending.toString) {
          throw InvalidInputException("Report Not Found or Already Processed")
        } else {
          val reporterID = decodeField[Int](json, "reporter_id")
          val videoID = decodeField[Int](json, "video_id")
          (reporterID, videoID)
        }
    }
  }

  private def validateVideo(videoID: Int)(using PlanContext): IO[(String, Int)] = {
    for {
      video <- QueryVideoInfoMessage(Some(token), videoID).send
    } yield (video.title, video.uploaderID)
  }

  private def updateReportStatus(reportID: Int, status: ReportStatus)(using
                                                                      PlanContext
  ): IO[String] = {
    for {
      _ <- IO(logger.info(s"更新举报记录状态为 $status"))
      writeResult <- writeDB(
        s"UPDATE $schemaName.report_video_table SET status = ? WHERE report_id = ?;",
        List(
          SqlParameter("String", status.toString),
          SqlParameter("Int", reportID.toString)
        )
      )
    } yield writeResult
  }
}