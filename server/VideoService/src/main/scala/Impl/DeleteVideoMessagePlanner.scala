package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.RecommendationService.DeleteVideoInfoMessage
import Common.APIException.InvalidInputException
import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * Planner for DeleteVideoMessage: 根据用户Token校验权限后，根据videoID删除视频记录
 */

case class DeleteVideoMessagePlanner(
    token: String,
    videoID: Int,
    override val planContext: PlanContext
) extends Planner[Unit] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = for {
    _ <- IO(logger.info(s"开始执行DeleteVideoMessagePlanner, token=$token, videoID=$videoID"))
    userID <- GetUIDByTokenMessage(token).send

    // Step 2: Validate video existence and fetch uploader ID
    _ <- IO(logger.info(s"开始校验视频ID是否合法, videoID=$videoID"))
    uploaderID <- fetchUploaderID(videoID)
    _ <- IO(logger.info(s"获取到视频的上传者ID: $uploaderID"))

    // Step 3: Check user permissions
    _ <- IO(logger.info(s"开始校验用户是否有权限删除该视频, userID=$userID, uploaderID=$uploaderID"))
    hasPermission <- checkPermissions(userID, uploaderID)
    _ <- IO(logger.info(s"权限校验结果: $hasPermission"))
    _ <- IO.raiseUnless(hasPermission)(InvalidInputException("Permission Denied"))

    // Step 4: Notify RecommendationService before deletion
    _ <- IO(logger.info(s"通知 RecommendationService 删除视频信息, videoID=$videoID"))
    _ <- notifyRecommendationService(videoID)

    // Step 5: Delete the video
    _ <- IO(logger.info(s"开始删除视频记录, videoID=$videoID"))
    _ <- deleteVideo(videoID)

  } yield ()


  /**
   * 校验视频ID是否存在，并获取上传者ID
   */
  private def fetchUploaderID(videoID: Int)(using PlanContext): IO[Int] = {
    val sql =
      s"""
        SELECT uploader_id
        FROM $schemaName.video_table
        WHERE video_id = ?;
      """
    readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
        case None => throw InvalidInputException("视频不存在")
        case Some(json) => decodeField[Int](json, "uploader_id")
      }
  }

  /**
   * 检查用户是否有权限删除视频
   */
  private def checkPermissions(
      userID: Int,
      uploaderID: Int
  )(using PlanContext): IO[Boolean] = {
    if (userID == uploaderID) {
      IO(true) // 上传者本人有权限
    } else {
      QueryUserRoleMessage(token).send.map {
        case UserRole.Auditor => true // 审核员有权限
        case _ => false
      }
    }
  }

  /**
   * 执行视频删除操作
   */
  private def deleteVideo(videoID: Int)(using PlanContext): IO[String] = {
    val sql =
      s"""
        DELETE FROM $schemaName.video_table
        WHERE video_id = ?;
      """
    writeDB(sql, List(SqlParameter("Int", videoID.toString)))
  }

  /**
   * 通知 RecommendationService 删除视频信息
   */
  private def notifyRecommendationService(videoID: Int)(using PlanContext): IO[Unit] = {
    IO(logger.info(s"[notifyRecommendationService] Notifying RecommendationService about video deletion: videoID=$videoID")) >> {
      DeleteVideoInfoMessage(token, videoID).send.handleErrorWith { error =>
        IO(logger.warn(s"Failed to notify RecommendationService: ${error.getMessage}")) >> IO.unit
      }
    }
  }
}