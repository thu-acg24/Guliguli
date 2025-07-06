package Impl

import APIs.UserService.{GetUIDByTokenMessage, QueryUserRoleMessage}
import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.{clientResource, minioClient, minioConfig, sessions}
import Objects.UserService.UserRole
import Objects.VideoService.UploadPath
import Objects.{CoverPayload, VideoPayload}
import Utils.VerifyProcess.*
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import com.google.common.collect.ArrayListMultimap
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.StatObjectArgs
import org.http4s.circe.jsonEncoder
import org.http4s.headers.`Content-Type`
import org.http4s.{MediaType, Method, Request, Uri}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import io.minio.messages.Part

import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.TimeUnit

case class ValidateVideoMessagePlanner(
    sessionToken: String,
    etags: List[String],
    override val planContext: PlanContext
) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    val etagArray = etags.zipWithIndex.map((etag, index) => Part(1 + index, etag)).toArray
    etagArray.foreach { part =>
      logger.info(s"Part(partNumber = ${part.partNumber()}, etag = ${part.etag()})")
    }
    for {
      newToken <- IO(UUID.randomUUID().toString)
      _ <- IO(logger.info(s"Validating token $sessionToken"))
      session <- IO.blocking(Option(sessions.getIfPresent(sessionToken))).flatMap{
        case Some(session) if !session.completed =>
          for {
            _ <- IO.fromCompletableFuture(
              IO.delay {
                minioClient.completeMultipartUploadAsync(
                  "temp",
                  "us-east-1",
                  session.objectName,
                  session.uploadID,
                  etagArray,
                  ArrayListMultimap.create[String, String](),
                  ArrayListMultimap.create[String, String]()
                )
              }
            ).handleErrorWith(ex =>
              IO.raiseError(InvalidInputException(s"结束上传时出现错误：${ex.getMessage}"))
            )
            _ <- IO.blocking {
              sessions.invalidate(session.token)
              sessions.put(newToken, session.copy(token = newToken, completed = true))
            }
             _ <- processUploadedFile(true, session.objectName)
          } yield session
        case _ =>
          IO.raiseError(InvalidInputException(s"不合法的sessionToken"))
      }
      _ <- sendMessage(true, newToken, session.videoID, session.objectName)
    } yield()
  }
  
}