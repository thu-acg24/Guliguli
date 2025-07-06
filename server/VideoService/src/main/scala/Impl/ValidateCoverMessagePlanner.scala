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
import Objects.{CoverPayload, VideoPayload}
import Objects.VideoService.UploadPath
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import com.google.common.collect.ArrayListMultimap
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.StatObjectArgs
import org.http4s.headers.`Content-Type`
import org.http4s.{MediaType, Method, Request, Uri}
import org.http4s.circe.jsonEncoder
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import Utils.VerifyProcess.*

import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.TimeUnit

case class ValidateCoverMessagePlanner(
    sessionToken: String,
    override val planContext: PlanContext
) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      newToken <- IO(UUID.randomUUID().toString)
      _ <- IO(logger.info(s"Validating token $sessionToken"))
      session <- IO.blocking(Option(sessions.getIfPresent(sessionToken))).flatMap{
        case Some(session) if !session.completed =>
          for {
            _ <- IO.blocking {
              sessions.invalidate(session.token)
              sessions.put(newToken, session.copy(token = newToken, completed = true))
            }
             _ <- processUploadedFile (false, session.objectName)
          } yield session
        case _ =>
          IO.raiseError(InvalidInputException(s"不合法的sessionToken"))
      }
      _ <- sendMessage(false, newToken, session.videoID, session.objectName)
    } yield()
  }

}