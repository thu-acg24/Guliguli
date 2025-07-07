package Impl


import APIs.UserService.{GetUIDByTokenMessage, QueryUserRoleMessage}
import APIs.RecommendationService.UpdateVideoInfoMessage
import Common.API.PlanContext
import Common.APIException.InvalidInputException
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Objects.RecommendationService.VideoInfo
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ModifyVideoMessagePlanner(
                                      token: String,
                                      videoID: Int,
                                      title: Option[String],
                                      description: Option[String],
                                      tag: Option[List[String]],
                                      override val planContext: PlanContext
                                    ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Start ModifyVideoMessagePlanner with token: $token, videoID: $videoID"))

      // Step 1: Validate token
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: Validate video existence and fetch uploader ID
      _ <- IO(logger.info(s"开始校验视频ID是否合法, videoID=$videoID"))
      (uploaderID, status) <- fetchUploaderIDAndStatus(videoID)
      _ <- IO(logger.info(s"获取到视频的上传者ID: $uploaderID 状态: $status"))

      // Step 3: Check user permissions
      _ <- IO(logger.info(s"开始校验用户是否有权限修改该视频, userID=$userID, uploaderID=$uploaderID"))
      hasPermission <- IO(userID == uploaderID)
      _ <- IO(logger.info(s"权限校验结果: $hasPermission"))
      _ <- IO.raiseUnless(hasPermission)(InvalidInputException("Permission Denied"))

      // Step 4: Update video fields
      _ <- updateVideo(videoID, title, description, tag, status)

      // Step 5: Notify RecommendationService about video update
      _ <- IO(logger.info(s"[notifyRecommendationService] Notifying RecommendationService about video update: videoID=$videoID"))
      _ <- UpdateVideoInfoMessage(token, videoID).send
    } yield ()
  }

  /**
   * 校验视频ID是否存在，并获取上传者ID与视频状态
   */
  private def fetchUploaderIDAndStatus(videoID: Int)(using PlanContext): IO[(Int, VideoStatus)] = {
    val sql =
      s"""
        SELECT uploader_id, status
        FROM $schemaName.video_table
        WHERE video_id = ?;
      """
    readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
      case None => throw InvalidInputException("视频不存在")
      case Some(json) => (decodeField[Int](json, "uploader_id"), decodeField[VideoStatus](json, "status"))
    }
  }

  private def updateVideo(
                           videoID: Int,
                           title: Option[String],
                           description: Option[String],
                           tag: Option[List[String]],
                           status: VideoStatus
                         )(using PlanContext): IO[String] = {
    for {
      currentTime <- IO(DateTime.now().getMillis.toString)
      updates <- IO {
        List(
          title.map { t => "title = ?" -> SqlParameter("String", t) },
          description.map { desc => "description = ?" -> SqlParameter("String", desc) },
          tag.map {t => "tag = ?" -> SqlParameter("Array[String]", t.asJson.noSpaces)},
          Some("status = ?" -> SqlParameter("String",
            if status == VideoStatus.Uploading then "Uploading" else "Pending")),
          Some("upload_time = ?" -> SqlParameter("DateTime", currentTime))
        ).flatten
      }
      
      result <- if (updates.isEmpty) {
        IO(logger.info(s"No fields to update for videoID: $videoID")).as("")
      } else {
        val (setClause, sqlParams) = updates.unzip
        val sql =
          s"""
             UPDATE $schemaName.video_table
             SET ${setClause.mkString(", ")}
             WHERE video_id = ?;
           """
        val parameters = sqlParams :+ SqlParameter("Int", videoID.toString)
        writeDB(sql, parameters)
      }
    } yield result
  }
}