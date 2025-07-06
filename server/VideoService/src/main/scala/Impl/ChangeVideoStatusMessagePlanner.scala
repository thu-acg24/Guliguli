package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.RecommendationService.UpdateVideoInfoMessage
import Common.APIException.InvalidInputException
import APIs.UserService.QueryUserRoleMessage
import APIs.VideoService.ChangeVideoStatusMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Objects.VideoService.VideoStatus
import Objects.RecommendationService.VideoInfo
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
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
          if (status != VideoStatus.Private) then IO.raiseError(InvalidInputException("权限不足"))
          else IO.unit
      }

      // Step 2: 验证视频ID的存在性和状态
      _ <- IO(logger.info("[validateVideoStatus] Validating videoID existence and status"))
      _ <- {
          val sql = s"SELECT uploader_id FROM ${schemaName}.video_table WHERE video_id = ?;"
          readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
            case Some(json) =>
              if (decodeField[Int](json, "uploader_id") != userID) then
                IO.raiseError(InvalidInputException("权限不足"))
              else IO.unit
            case None => throw InvalidInputException("找不到视频")
          }
        }
      
      // Step 3: 修改视频状态
      _ <- IO(logger.info(s"[Step 3] 开始修改视频状态为: $status..."))
      _ <- updateVideoStatus(videoID, status)

      // Step 4: 通知 RecommendationService 更新视频可见性
      _ <- IO(logger.info(s"[notifyRecommendationService] Notifying RecommendationService: videoID=${videoID}"))
      _ <- UpdateVideoInfoMessage(token, videoID).send

    } yield ()
  }

  private def updateVideoStatus(videoID: Int, status: VideoStatus)(using PlanContext): IO[String] = {
    IO(logger.info(s"[updateVideoStatus] Updating video status for videoID=${videoID} to status=${status}")) >> {
      val updateSql = s"UPDATE ${schemaName}.video_table SET status = ? WHERE video_id = ?;"
      writeDB(updateSql, List(
        SqlParameter("String", status.toString),
        SqlParameter("Int", videoID.toString)
      ))
    }
  }
}