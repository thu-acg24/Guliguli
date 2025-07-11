package Impl

import APIs.RecommendationService.UpdateVideoInfoMessage
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
import Utils.MediaProcess.*
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import com.google.common.collect.{ArrayListMultimap, Multimap}
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.http.Method
import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryUploadVideoPathMessagePlanner(
    token: String,
    videoID: Int,
    partCount: Int,
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
      uploaderID <- validateVideo(videoID)
      _ <- IO.raiseUnless(userID == uploaderID)(InvalidInputException("上传者不是视频发布者"))

      // Step 3: set status to Uploading
      _ <- updateVideoStatus(videoID)
      _ <- UpdateVideoInfoMessage(token, videoID).send

      // Step 3: Generate MinIO links
      videoName <- generateObjectName(videoID, "video")
      (uploadID, videoUploadUrls) <- generateUploadUrls(videoName)
      videoToken <- IO(UUID.randomUUID().toString)
      _ <- IO(sessions.put(videoToken, UploadSession(videoToken, videoID, videoName, uploadID)))
    } yield UploadPath(videoUploadUrls, videoToken)
  }

  private def generateUploadUrls(videoName: String): IO[(String, List[String])] = {
    for {
      uploadID <- IO.fromCompletableFuture(
          IO.delay {
            minioClient.createMultipartUploadAsync(
              "temp",
              "us-east-1",
              videoName,
              ArrayListMultimap.create[String, String](),
              ArrayListMultimap.create[String, String]()
//              CreateMultipartUploadArgs.builder() // 注意是 CreateMultipartUploadArgs
//                .bucket(bucketName)
//                .`object`(videoName)
//                .build()
            )
          }
        ).map(_.result().uploadId()) // 从 CompleteMultipartUploadResponse 提取 uploadId
      urls <- (0 until partCount).toList
        .parTraverse { partNumber => // 并行处理
          generateUploadUrl(videoName, Map(
            "uploadId" -> uploadID,
            "partNumber" -> (partNumber + 1).toString
          ).asJava)
        }
    } yield (uploadID, urls)
  }

}