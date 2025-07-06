package Impl

import APIs.RecommendationService.UpdateVideoInfoMessage
import APIs.UserService.{GetUIDByTokenMessage, QueryUserRoleMessage}
import APIs.VideoService.ChangeVideoStatusMessage
import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.RecommendationService.VideoInfo
import Objects.UserService.UserRole
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class FinishUploadingMessagePlanner(
    token: String,
    videoID: Int
) extends Planner[Unit] {
  private val logger =
    LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate token and retrieve uploaderID
      _ <- IO(logger.info("Step 1: 校验Token是否合法并获取用户ID"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: 验证视频ID的存在性和状态
      _ <- IO(logger.info("[validateVideoStatus] Validating videoID existence and status"))
      _ <- validateVideo(userID)
      
      // Step 3: 修改视频状态
      _ <- updateVideoStatus()
    } yield ()
  }

  private def validateVideo(userID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""SELECT COUNT(*) > 0 FROM ${schemaName}.video_table WHERE video_id = ?
         |AND cover IS NOT NULL AND m3u8_name IS NOT NULL
         |AND uploader_id = ?
         |""".stripMargin
    val param = List(SqlParameter("Int", videoID.toString), SqlParameter("Int", userID.toString))
    for {
      exists <- readDBBoolean(sql, param)
      _ <- IO.raiseUnless(exists)(InvalidInputException("视频不存在或封面/视频未上传"))
    } yield ()
  }

  private def updateVideoStatus()(using PlanContext): IO[String] = {
    IO(logger.info(s"[updateVideoStatus] Updating video status for videoID=${videoID} to status=Pending")) >> {
      val updateSql = s"UPDATE ${schemaName}.video_table SET status = ? WHERE video_id = ?;"
      writeDB(updateSql, List(
        SqlParameter("String", VideoStatus.Pending.toString),
        SqlParameter("Int", videoID.toString)
      ))
    }
  }
}