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
import Objects.VideoService.Video
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
  val logger =
    LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {

      // Step 1: 验证Token并获取用户ID
      _ <- IO(logger.info(s"调用QueryUserRoleMessage校验用户审核员权限..."))
      role <- QueryUserRoleMessage(token).send
      _ <- IO(logger.info(s"[Substep 1.2] 获得用户角色: ${role.toString}"))
      _ <- role match {
        case UserRole.Auditor => IO.unit
        case _ => throw InvalidInputException("权限不足")
      }

      // Step 2: 验证视频ID的存在性和状态
      _ <- IO(logger.info("[validateVideoStatus] Validating videoID existence and status"))
      _ <- {
          val sql = s"SELECT status FROM ${schemaName}.video_table WHERE video_id = ?;"
          readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
            case Some(json) => ()
            case None => throw InvalidInputException("找不到视频")
          }
        }
      
      // Step 3: 修改视频状态
      _ <- IO(logger.info(s"[Step 3] 开始修改视频状态为: $status..."))
      _ <- updateVideoStatus(videoID, status)

      // Step 4: 通知 RecommendationService 更新视频可见性
      _ <- notifyRecommendationService(videoID, status)

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

  private def notifyRecommendationService(videoID: Int, status: VideoStatus)(using PlanContext): IO[Unit] = {
    IO(logger.info(s"[notifyRecommendationService] Notifying RecommendationService about video status change: videoID=${videoID}, status=${status}")) >> {
      // 获取视频详细信息
      getVideoInfo(videoID).flatMap { videoInfo =>
        // 根据视频状态决定可见性：只有 Approved 状态的视频才可见
        val visible = status == VideoStatus.Approved
        val updatedVideoInfo = videoInfo.copy(visible = visible)
        
        UpdateVideoInfoMessage(token, updatedVideoInfo).send.handleErrorWith { error =>
          IO(logger.warn(s"Failed to notify RecommendationService: ${error.getMessage}")) >> IO.unit
        }
      }
    }
  }

  private def getVideoInfo(videoID: Int)(using PlanContext): IO[VideoInfo] = {
    IO(logger.info(s"[getVideoInfo] Fetching video info for videoID=${videoID}")) >> {
      val sql = s"SELECT video_id, title, description, tag, uploader_id, views, likes, favorites FROM ${schemaName}.video_table WHERE video_id = ?;"
      readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
        case Some(json) =>
          VideoInfo(
            videoID = decodeField[Int](json, "video_id"),
            title = decodeField[String](json, "title"),
            description = decodeField[String](json, "description"),
            tag = decodeField[String](json, "tag").split(",").toList.map(_.trim).filter(_.nonEmpty),
            uploaderID = decodeField[Int](json, "uploader_id"),
            views = decodeField[Int](json, "views"),
            likes = decodeField[Int](json, "likes"),
            favorites = decodeField[Int](json, "favorites"),
            visible = true // 默认值，后续会根据状态更新
          )
        case None => throw InvalidInputException("找不到视频")
      }
    }
  }
}