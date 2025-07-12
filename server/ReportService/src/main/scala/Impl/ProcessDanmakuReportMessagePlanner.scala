package Impl


import APIs.DanmakuService.DeleteDanmakuMessage
import Common.APIException.InvalidInputException
import APIs.DanmakuService.QueryDanmakuByIDMessage
import APIs.MessageService.SendNotificationMessage
import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserRoleMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.DanmakuService.Danmaku
import Objects.MessageService.Message
import Objects.ReportService.ReportStatus
import Objects.UserService.UserRole
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import Utils.ValidateProcess.validateTokenAndRole
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ProcessDanmakuReportMessagePlanner(
                                               token: String,
                                               reportID: Int,
                                               status: ReportStatus,
                                               override val planContext: PlanContext
                                             ) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    if status == ReportStatus.Pending then return IO.raiseError(InvalidInputException("不允许改为等待状态"))
    if status == ReportStatus.Rejected then for {
      _ <- IO(logger.info("开始处理 ProcessDanmakuReportMessage 请求"))
      // 特判被拒绝的举报
      _ <- validateTokenAndRole(token)
      (reporterID, danmakuID) <- validateReportID(reportID)
      _ <- updateReportStatus(reportID, status)
    } yield ()
    for {
      _ <- IO(logger.info("开始处理 ProcessDanmakuReportMessage 请求"))
      // Step 1: 校验 token 是否有效
      _ <- validateTokenAndRole(token)
      (reporterID, danmakuID) <- validateReportID(reportID)
      (danmakuContent, danmakuAuthorID, videoTitle, videoID) <- validateDanmakuAndVideo(danmakuID)
      _ <- updateReportStatus(reportID, status)
      _ <- DeleteDanmakuMessage(token, danmakuID).send
      _ <- SendNotificationMessage(token, reporterID,
        s"举报处理通知",
        s"您在视频 $videoTitle 下举报的弹幕 $danmakuContent 已被处理").send
      _ <- SendNotificationMessage(token, danmakuAuthorID,
          s"弹幕违规通知",
          s"您在视频 $videoTitle 下的弹幕 $danmakuContent 被举报并已被审核员删除").send
    } yield ()
  }

  private def validateReportID(
                                   reportID: Int
                                 )(using PlanContext): IO[(Int, Int)] = {
    for {
      _ <- IO(logger.info(s"校验 reportID [$reportID] 是否存在"))
      result <- readDBJsonOptional(
        s"SELECT reporter_id, danmaku_id, status FROM $schemaName.report_danmaku_table WHERE report_id = ?;",
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
          val danmakuID = decodeField[Int](json, "danmaku_id")
          (reporterID, danmakuID)
        }
    }
  }
  private def validateDanmakuAndVideo(
                                       danmakuID: Int
                                     )(using PlanContext): IO[(String, Int, String, Int)] = {
    for {
      _ <- IO(logger.info(s"校验弹幕 [$danmakuID] 和其所属视频是否存在"))
      comment <- QueryDanmakuByIDMessage(danmakuID).send
      videoID = comment.videoID
      video <- QueryVideoInfoMessage(Some(token), videoID).send
    } yield (comment.content, comment.authorID, video.title, videoID)
  }

  private def updateReportStatus(reportID: Int, status: ReportStatus)(using
                                                                      PlanContext
  ): IO[String] = {
    for {
      _ <- IO(logger.info(s"更新举报记录状态为 $status"))
      writeResult <- writeDB(
        s"UPDATE $schemaName.report_danmaku_table SET status = ? WHERE report_id = ?;",
        List(
          SqlParameter("String", status.toString),
          SqlParameter("Int", reportID.toString)
        )
      )
    } yield writeResult
  }
}