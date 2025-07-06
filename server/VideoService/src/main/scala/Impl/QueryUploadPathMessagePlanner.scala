package Impl

import APIs.UserService.{GetUIDByTokenMessage, QueryUserRoleMessage}
import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.{minioClient, sessions}
import Objects.UploadSession
import Objects.VideoService.UploadPath
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.http.Method
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import java.util.UUID
import java.util.concurrent.TimeUnit

case class QueryUploadPathMessagePlanner(
    token: String,
    videoID: Int,
    override val planContext: PlanContext
) extends Planner[UploadPath] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[UploadPath] = {
    for {
      // Step 1: Validate token and retrieve uploaderID
      _ <- IO(logger.info("Step 1: 校验Token是否合法并获取用户ID"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: Validate uploaderID
      _ <- IO(logger.info("Step 2: 校验上传者身份"))
      uploaderID <- validateVideo()
      _ <- IO.raiseUnless(userID == uploaderID)(InvalidInputException("上传者不是视频发布者"))

      // Step 3: set status to Uploading
      _ <- updateVideoStatus()

      // Step 3: Generate MinIO links
      coverName <- generateObjectName(videoID, "cover")
      coverUploadUrl <- generateUploadUrl(coverName)
      coverToken <- IO(UUID.randomUUID().toString)
      _ <- IO(sessions.put(coverToken, UploadSession(coverToken, videoID, coverUploadUrl)))
      videoName <- generateObjectName(videoID, "video")
      videoUploadUrl <- generateUploadUrl(videoName)
      videoToken <- IO(UUID.randomUUID().toString)
      _ <- IO(sessions.put(videoToken, UploadSession(videoToken, videoID, videoUploadUrl)))
    } yield UploadPath(coverUploadUrl, coverToken, videoUploadUrl, videoToken)
  }

  private def validateVideo()(using PlanContext): IO[Int] = {
    val sql = s"SELECT uploader_id FROM ${schemaName}.video_table WHERE video_id = ?;"
    val param = List(SqlParameter("Int", videoID.toString))
    readDBJsonOptional(sql, param).map {
      case None => throw InvalidInputException("视频不存在")
      case Some(json) => decodeField[Int](json, "uploader_id")
    }
  }

  private def updateVideoStatus()(using PlanContext): IO[String] = {
    IO(logger.info(s"[updateVideoStatus] Updating video status for videoID=${videoID} to status=Uploading")) >> {
      val updateSql = s"UPDATE ${schemaName}.video_table SET status = 'Uploading' WHERE video_id = ?;"
      writeDB(updateSql, List(
        SqlParameter("Int", videoID.toString)
      ))
    }
  }

  private def generateObjectName(userID: Int, info: String): IO[String] = {
    for {
      timestamp <- IO.realTimeInstant.map(_.toEpochMilli)
      random <- Random.scalaUtilRandom[IO].flatMap(_.betweenInt(0, 10000))
    } yield s"$userID/$timestamp-$info-$random.jpg"
  }

  private def generateUploadUrl(objectName: String): IO[String] = {
    IO.blocking { // 包装阻塞IO操作
      minioClient.getPresignedObjectUrl(
        io.minio.GetPresignedObjectUrlArgs.builder()
          .method(Method.PUT)
          .bucket("temp")
          .`object`(objectName)
          .expiry(15, TimeUnit.MINUTES)
          .build()
      )
    }
  }
}