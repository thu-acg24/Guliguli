package Impl


import APIs.MessageService.SendNotificationMessage
import APIs.UserService.GetUIDByTokenMessage
import APIs.RecommendationService.UpdateVideoInfoMessage
import Common.APIException.InvalidInputException
import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import org.slf4j.LoggerFactory

case class ChangeVideoStatusMessagePlanner(
    token: String,
    videoID: Int,
    status: VideoStatus,
    override val planContext: PlanContext
) extends Planner[Unit] {
  private val logger =
    LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    if status == VideoStatus.Uploading then return IO.raiseError(InvalidInputException("请使用换源API"))
    for {
      // Step 1: 验证Token并获取用户ID
      userID <- GetUIDByTokenMessage(token).send
      _ <- IO(logger.info(s"调用QueryUserRoleMessage校验用户审核员权限..."))
      role <- QueryUserRoleMessage(token).send
      _ <- IO(logger.info(s"[Substep 1.2] 获得用户角色: ${role.toString}"))
      _ <- role match {
        case UserRole.Auditor => IO.unit
        case _ =>
          if (status != VideoStatus.Private && status != VideoStatus.Pending) then
            IO.raiseError(InvalidInputException("权限不足"))
          else IO.unit
      }

      // Step 2: 验证视频ID的存在性和状态
      _ <- IO(logger.info("[validateVideoStatus] Validating videoID existence and status"))
      (uploaderID, title) <- {
          val sql = s"SELECT uploader_id, title, status FROM $schemaName.video_table WHERE video_id = ?;"
          readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).flatMap {
            case Some(json) =>
              val uploaderID = decodeField[Int](json, "uploader_id")
              val title = decodeField[String](json, "title")
              val originStatus = decodeField[VideoStatus](json, "status")
              if (List(VideoStatus.Uploading, VideoStatus.Broken).contains(status)) then
                IO.raiseError(InvalidInputException("不能修改处于系统状态(Uploading, Broken)的视频"))
              else if (uploaderID != userID && role != UserRole.Auditor) then
                IO.raiseError(InvalidInputException("权限不足"))
              else IO.pure((uploaderID, title))
            case None => IO.raiseError(InvalidInputException("找不到视频"))
          }
        }
      
      // Step 3: 修改视频状态
      _ <- IO(logger.info(s"[Step 3] 开始修改视频状态为: $status..."))
      _ <- updateVideoStatus(videoID, status)

      // Step 4: 通知 RecommendationService 更新视频可见性
      _ <- IO(logger.info(s"[notifyRecommendationService] Notifying RecommendationService: videoID=$videoID"))
      _ <- UpdateVideoInfoMessage(token, videoID).send

      _ <- role match {
        case UserRole.Auditor =>
          if (status == VideoStatus.Approved) then
            SendNotificationMessage(token, uploaderID, "你的视频已通过审核", s"恭喜，你的视频(标题：$title)已经通过审核！").send
          else if (status == VideoStatus.Rejected) then
            SendNotificationMessage(token, uploaderID, "你的视频未通过审核，已被下架", s"很抱歉，您的视频(标题：$title)经过审核后确认存在不合规内容，请进行整改后重新上传。").send
          else IO.unit
        case _ => IO.unit
      }

    } yield ()
  }

  private def updateVideoStatus(videoID: Int, status: VideoStatus)(using PlanContext): IO[String] = {
    IO(logger.info(s"[updateVideoStatus] Updating video status for videoID=$videoID to status=$status")) >> {
      val updateSql = s"UPDATE $schemaName.video_table SET status = ? WHERE video_id = ?;"
      writeDB(updateSql, List(
        SqlParameter("String", status.toString),
        SqlParameter("Int", videoID.toString)
      ))
    }
  }
}