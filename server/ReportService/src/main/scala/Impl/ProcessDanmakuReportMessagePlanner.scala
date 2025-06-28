package Impl


import APIs.DanmakuService.{DeleteDanmakuMessage, QueryDanmakuByIDMessage}
import APIs.UserService.{getUIDByTokenMessage, QueryUserRoleMessage}
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.DanmakuService.Danmaku
import Objects.ReportService.ReportStatus
import Objects.UserService.UserRole
import Objects.VideoService.{Video, VideoStatus}
import Utils.NotifyProcess.sendNotification
import cats.effect.IO
import io.circe.Json
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
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
import Objects.VideoService.VideoStatus
import Objects.MessageService.Message
import APIs.DanmakuService.QueryDanmakuByIDMessage
import Objects.VideoService.Video
import APIs.UserService.QueryUserRoleMessage
import APIs.DanmakuService.DeleteDanmakuMessage
import APIs.UserService.getUIDByTokenMessage
import io.circe._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import APIs.UserService.getUIDByTokenMessage

case class ProcessDanmakuReportMessagePlanner(
                                               token: String,
                                               reportID: Int,
                                               status: ReportStatus
                                             )(using val planContext: PlanContext) extends Planner[Option[String]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: 校验token是否有效
      _ <- IO(logger.info(s"开始校验用户Token: ${token}"))
      userIDOpt <- getUIDByTokenMessage(token).send
      result <- userIDOpt match {
        case None =>
          IO(logger.error("用户Token无效")) *> IO.pure(Some("Unauthorized Access"))
        case Some(userID) =>
          validateAuditor(token, userID)
      }
    } yield result
  }

  private def validateAuditor(token: String, userID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      userRoleOpt <- QueryUserRoleMessage(token).send
      result <- userRoleOpt match {
        case Some(UserRole.Auditor) =>
          checkReportExists()
        case _ =>
          IO(logger.error("非法访问，用户不是审核员")) *> IO.pure(Some("Unauthorized Access"))
      }
    } yield result
  }

  private def checkReportExists()(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"检查ReportID: ${reportID} 是否存在"))
      reportOpt <- readDBJsonOptional(
        s"SELECT * FROM ${schemaName}.report_danmaku_table WHERE report_id = ?;",
        List(SqlParameter("Int", reportID.toString))
      )
      result <- reportOpt match {
        case None =>
          IO(logger.error("ReportID不存在或已处理")) *> IO.pure(Some("Report Not Found or Already Processed"))
        case Some(report) =>
          validateDanmakuAndVideo(report)
      }
    } yield result
  }

  private def validateDanmakuAndVideo(report: Json)(using PlanContext): IO[Option[String]] = {
    for {
      reporterID <- IO(decodeField[Int](report, "reporter_id"))
      danmakuID <- IO(decodeField[Int](report, "danmaku_id"))
      // 检查弹幕
      _ <- IO(logger.info(s"检查弹幕ID: ${danmakuID}是否存在"))
      danmakuOpt <- QueryDanmakuByIDMessage(danmakuID).send
      result <- danmakuOpt match {
        case None =>
          IO(logger.error("弹幕不存在")) *> IO.pure(Some("Danmaku does not exist"))
        case Some(danmaku) =>
          validateVideo(danmaku, reporterID)
      }
    } yield result
  }

  private def validateVideo(danmaku: Danmaku, reporterID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"检查视频ID: ${danmaku.videoID}的状态"))
      videoOpt <- QueryVideoInfoMessage(token = None, danmaku.videoID).send
      result <- videoOpt match {
        case Some(video) if video.status == VideoStatus.Approved =>
          processReport(danmaku, video, reporterID)
        case _ =>
          IO(logger.error("视频不是公开状态")) *> IO.pure(Some("Video is not public"))
      }
    } yield result
  }

  private def processReport(danmaku: Danmaku, video: Video, reporterID: Int)(using PlanContext): IO[Option[String]] = {
    for {
      // Step 4: 删除弹幕 (如果状态为Resolved).
      _ <- if (status == ReportStatus.Resolved) {
        IO(logger.info(s"开始删除弹幕ID: ${danmaku.danmakuID}")) >>
          DeleteDanmakuMessage(token, danmaku.danmakuID).send.flatMap {
            case Some(error) =>
              IO(logger.error(s"删除弹幕失败: ${error}")) *> IO.pure(Some("Failed to delete danmaku"))
            case None =>
              IO(logger.info(s"弹幕ID: ${danmaku.danmakuID} 删除成功"))
          }
      } else IO.unit

      // Step 5: 更新举报记录状态.
      _ <- IO(logger.info(s"更新ReportID: ${reportID} 的状态为: ${status}"))
      updateResult <- writeDB(
        s"UPDATE ${schemaName}.report_danmaku_table SET status = ? WHERE report_id = ?;",
        List(
          SqlParameter("String", status.toString),
          SqlParameter("Int", reportID.toString)
        )
      )
      _ <- if (updateResult != "Operation(s) done successfully") {
        IO(logger.error(s"更新举报记录状态失败")) >> IO.pure(Some("Failed to process report"))
      } else IO.unit

      // Step 6: 向用户发送通知.
      _ <- IO(logger.info("发送举报处理结果给用户"))
      notificationContent = s"举报结果: 视频标题: ${video.title}, 弹幕内容: ${danmaku.content}, 状态: ${status}"
      notificationResult <- sendNotification(token, reporterID, notificationContent)
      _ <- notificationResult match {
        case Some(error) =>
          IO(logger.error(s"发送通知失败: ${error}")) *> IO.pure(Some(s"Notification failed: ${error}"))
        case None =>
          IO(logger.info("通知发送成功"))
      }

      // Step 7: 返回操作结果.
      _ <- IO(logger.info("操作成功"))
    } yield None
  }
}