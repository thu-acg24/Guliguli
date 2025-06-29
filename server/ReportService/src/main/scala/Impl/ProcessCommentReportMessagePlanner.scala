package Impl


import Objects.VideoService.VideoStatus
import Objects.MessageService.Message
import Objects.CommentService.Comment
import Objects.VideoService.Video
import APIs.CommentService.QueryCommentByIDMessage
import Objects.UserService.UserRole
import Utils.NotifyProcess.sendNotification
import APIs.UserService.QueryUserRoleMessage
import APIs.VideoService.QueryVideoInfoMessage
import APIs.UserService.GetUIDByTokenMessage
import APIs.CommentService.DeleteCommentMessage
import Objects.ReportService.ReportStatus
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
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
import Objects.ReportService.ReportStatus
import cats.implicits._
import io.circe._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ProcessCommentReportMessagePlanner(
    token: String,
    reportID: Int,
    status: ReportStatus,
    override val planContext: PlanContext
) extends Planner[Option[String]] {

  private val logger =
    LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info("开始处理 ProcessCommentReportMessage 请求"))
      // Step 1: 校验 token 是否有效
      roleOption <- validateTokenAndRole(token)
      result <- roleOption match {
        case None =>
          IO(logger.error("用户权限校验失败，返回未授权访问")) >>
            IO.pure(Some("Unauthorized Access"))
        case Some(_) =>
          // Step 2: 校验 reportID 是否存在
          validateReportResult <- validateReportID(reportID)
          validateReportResult match {
            case Left(errorMsg) =>
              IO(logger.error(s"校验 reportID 失败：${errorMsg}")) >>
                IO.pure(Some(errorMsg))
            case Right((reporterID, commentID)) =>
              // Step 3: 检查评论和视频是否存在
              validateCommentResult <- validateCommentAndVideo(commentID)
              validateCommentResult match {
                case Left(errorMsg) =>
                  IO(logger.error(s"校验评论和视频失败：${errorMsg}")) >>
                    IO.pure(Some(errorMsg))
                case Right((commentContent, videoTitle, videoID)) =>
                  // Step 4: 删除评论（根据状态决定是否删除）
                  deletionResult <- deleteCommentIfNeeded(commentID)
                  deletionResult match {
                    case Some(errorMsg) =>
                      IO(logger.error(s"删除评论失败：${errorMsg}")) >>
                        IO.pure(Some(errorMsg))
                    case None =>
                      // Step 5: 更新举报记录状态
                      updateReportResult <- updateReportStatus(reportID, status)
                      updateReportResult match {
                        case Some(errorMsg) =>
                          IO(logger.error(s"更新举报记录状态失败：${errorMsg}")) >>
                            IO.pure(Some(errorMsg))
                        case None =>
                          // Step 6: 向用户发送通知
                          notificationResult <- sendNotificationToReporter(
                            token,
                            reporterID,
                            videoTitle,
                            commentContent,
                            status
                          )
                          if (notificationResult.isDefined) {
                            IO(logger.error(s"发送通知失败：${notificationResult.get}"))
                          }
                          IO.pure(notificationResult)
                      }
                  }
              }
          }
      }
    } yield result
  }

  private def validateTokenAndRole(
      token: String
  )(using PlanContext): IO[Option[UserRole]] = {
    for {
      _ <- IO(logger.info("开始校验 token 和用户角色"))
      userRoleOption <- QueryUserRoleMessage(token).send
      _ <- IO(userRoleOption match {
        case Some(UserRole.Auditor) =>
          logger.info("用户角色校验通过")
        case _ =>
          logger.error("用户角色校验失败")
      })
    } yield userRoleOption.collect { case UserRole.Auditor => UserRole.Auditor }
  }

  private def validateReportID(
      reportID: Int
  )(using PlanContext): IO[Either[String, (Int, Int)]] = {
    for {
      _ <- IO(logger.info(s"校验 reportID [${reportID}] 是否存在"))
      result <- readDBJsonOptional(
        s"SELECT reporter_id, comment_id, status FROM ${schemaName}.report_comment_table WHERE report_id = ?;",
        List(SqlParameter("Int", reportID.toString))
      )
    } yield result match {
      case None =>
        Left("Report Not Found or Already Processed")
      case Some(json) =>
        val status = decodeField[String](json, "status")
        if (status != ReportStatus.Pending.toString) {
          Left("Report Not Found or Already Processed")
        } else {
          val reporterID = decodeField[Int](json, "reporter_id")
          val commentID = decodeField[Int](json, "comment_id")
          Right((reporterID, commentID))
        }
    }
  }

  private def validateCommentAndVideo(
      commentID: Int
  )(using PlanContext): IO[Either[String, (String, String, Int)]] = {
    for {
      _ <- IO(logger.info(s"校验评论 [${commentID}] 和其所属视频是否存在"))
      commentOption <- QueryCommentByIDMessage(commentID).send
      result <- commentOption match {
        case None =>
          IO.pure(Left("Comment does not exist"))
        case Some(comment) =>
          val videoID = comment.videoID
          for {
            videoOption <- QueryVideoInfoMessage(Some(token), videoID).send
          } yield videoOption match {
            case None =>
              Left("Video does not exist")
            case Some(video) if video.status != VideoStatus.Approved =>
              Left("Video is not public")
            case Some(video) =>
              Right((comment.content, video.title, videoID))
          }
      }
    } yield result
  }

  private def deleteCommentIfNeeded(commentID: Int)(using PlanContext): IO[Option[String]] = {
    if (status == ReportStatus.Resolved) {
      DeleteCommentMessage(token, commentID).send
    } else {
      IO.pure(None)
    }
  }

  private def updateReportStatus(reportID: Int, status: ReportStatus)(using
      PlanContext
  ): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"更新举报记录状态为 ${status}"))
      writeResult <- writeDB(
        s"UPDATE ${schemaName}.report_comment_table SET status = ? WHERE report_id = ?;",
        List(
          SqlParameter("String", status.toString),
          SqlParameter("Int", reportID.toString)
        )
      )
    } yield {
      if (writeResult != "Operation(s) done successfully") {
        Some("Failed to process report")
      } else {
        None
      }
    }
  }

}