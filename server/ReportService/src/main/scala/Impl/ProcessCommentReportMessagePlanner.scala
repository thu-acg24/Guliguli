package Impl


import APIs.CommentService.DeleteCommentMessage
import Common.APIException.InvalidInputException
import APIs.CommentService.QueryCommentByIDMessage
import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserRoleMessage
import APIs.VideoService.QueryVideoInfoMessage
import APIs.MessageService.SendNotificationMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.CommentService.Comment
import Objects.MessageService.Message
import Objects.ReportService.ReportStatus
import Objects.UserService.UserRole
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import Utils.ValidateProcess.validateTokenAndRole
import cats.effect.IO
import cats.implicits.*
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ProcessCommentReportMessagePlanner(
    token: String,
    reportID: Int,
    status: ReportStatus,
    override val planContext: PlanContext
) extends Planner[Unit] {

  private val logger =
    LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info("开始处理 ProcessCommentReportMessage 请求"))
      // Step 1: 校验 token 是否有效
      _ <- validateTokenAndRole(token)
      (reporterID, commentID) <- validateReportID(reportID)
      (commentContent, commentAuthorID, videoTitle, videoID) <- validateCommentAndVideo(commentID)
      _ <- updateReportStatus(reportID, status)
      _ <- deleteCommentIfNeeded(commentID)
      _ <- SendNotificationMessage(token, reporterID, s"您在视频 ${videoTitle} 下举报的评论 ${commentContent} 已被处理").send
      _ <- status match {
        case ReportStatus.Resolved => SendNotificationMessage(token, commentAuthorID,
          s"您在视频 ${videoTitle} 下的评论 ${commentContent} 被举报并已被审核员删除").send
        case _ => IO.unit
      }
    } yield ()
  }

  private def validateReportID(
      reportID: Int
  )(using PlanContext): IO[(Int, Int)] = {
    for {
      _ <- IO(logger.info(s"校验 reportID [${reportID}] 是否存在"))
      result <- readDBJsonOptional(
        s"SELECT reporter_id, comment_id, status FROM ${schemaName}.report_comment_table WHERE report_id = ?;",
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
          val commentID = decodeField[Int](json, "comment_id")
          (reporterID, commentID)
        }
    }
  }

  private def validateCommentAndVideo(
      commentID: Int
  )(using PlanContext): IO[(String, Int, String, Int)] = {
    for {
      _ <- IO(logger.info(s"校验评论 [${commentID}] 和其所属视频是否存在"))
      comment <- QueryCommentByIDMessage(commentID).send
      videoID = comment.videoID
      video <- QueryVideoInfoMessage(Some(token), videoID).send
    } yield (comment.content, comment.authorID, video.title, videoID)
  }

  private def deleteCommentIfNeeded(commentID: Int)(using PlanContext): IO[Unit] = {
    if (status == ReportStatus.Resolved) {
      DeleteCommentMessage(token, commentID).send
    } else {
      IO.unit
    }
  }

  private def updateReportStatus(reportID: Int, status: ReportStatus)(using
      PlanContext
  ): IO[String] = {
    for {
      _ <- IO(logger.info(s"更新举报记录状态为 ${status}"))
      writeResult <- writeDB(
        s"UPDATE ${schemaName}.report_comment_table SET status = ? WHERE report_id = ?;",
        List(
          SqlParameter("String", status.toString),
          SqlParameter("Int", reportID.toString)
        )
      )
    } yield writeResult
  }

}