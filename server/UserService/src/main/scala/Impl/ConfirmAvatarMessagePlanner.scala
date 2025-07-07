package Impl

import APIs.MessageService.SendNotificationMessage
import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.{minioClient, sessions}
import Objects.UploadSession
import Objects.UserService.UserInfo
import Utils.AuthProcess.validateToken
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.{CopyObjectArgs, CopySource, StatObjectArgs}
import io.minio.http.Method
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.TimeUnit

case class ConfirmAvatarMessagePlanner(
                                         sessionToken: String,
                                         status: String,
                                         objectName: String,
                                         override val planContext: PlanContext
                                       ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan definition
  override def plan(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Validating token $sessionToken"))
      session <- IO(Option(sessions.getIfPresent(sessionToken))).flatMap{
        case Some(session) if session.completed =>
          IO.pure(session)
        case _ =>
          IO.raiseError(InvalidInputException(s"不合法的sessionToken"))
      }
      _ <- IO(sessions.invalidate(session.token))
      _ <- status match {
        case "success" => updateAvatarLinkInDB(session.userID, objectName)
        case "failure" => updateAvatarLinkInDB(session.userID, "image_fallback.jpg")
        case _ => IO.raiseError(InvalidInputException(s"status必须是success或failure中的一个"))
      }
    } yield()
  }

  private def updateAvatarLinkInDB(userID: Int, objectName: String)(using PlanContext): IO[Unit] = {
    val querySQL =
      s"""
           UPDATE $schemaName.user_table
           SET avatar_path = ?
           WHERE user_id = ?
         """.stripMargin

    val queryParams = List(
      SqlParameter("String", objectName),
      SqlParameter("Int", userID.toString)
    )

    for {
      _ <- IO(logger.info(s"Executing update query: $querySQL with params: $queryParams"))
      updateResponse <- writeDB(querySQL, queryParams)
    } yield ()
  }
}