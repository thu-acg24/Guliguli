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
import Utils.MediaProcess.*
import java.util.UUID
import java.util.concurrent.TimeUnit

case class QueryUploadCoverPathMessagePlanner(
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
      uploaderID <- validateVideo(videoID)
      _ <- IO.raiseUnless(userID == uploaderID)(InvalidInputException("上传者不是视频发布者"))

      // Step 3: set status to Uploading
      _ <- updateVideoStatus(videoID)

      // Step 3: Generate MinIO links
      coverName <- generateObjectName(videoID, "cover")
      coverUploadUrl <- generateUploadUrl(coverName)
      coverToken <- IO(UUID.randomUUID().toString)
      _ <- IO(sessions.put(coverToken, UploadSession(coverToken, videoID, coverName)))
    } yield UploadPath(List(coverUploadUrl), coverToken)
  }
}