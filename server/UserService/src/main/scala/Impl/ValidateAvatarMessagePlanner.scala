package Impl

import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.APIException.InvalidInputException
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.{clientResource, minioClient, sessions, minioConfig}
import Objects.UploadSession
import Objects.UserService.UserInfo
import Utils.AuthProcess.validateToken
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.StatObjectArgs
import io.minio.CopyObjectArgs
import io.minio.CopySource
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import org.http4s._

import java.util.UUID
import java.util.concurrent.TimeUnit
import java.text.DecimalFormat

case class ValidateAvatarMessagePlanner(
                                         sessionToken: String,
                                         override val planContext: PlanContext
                                       ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan definition
  override def plan(using PlanContext): IO[Unit] = {
    for {
      newToken <- IO(UUID.randomUUID().toString)
      _ <- IO(logger.info(s"Validating token $sessionToken"))
      session <- IO(Option(sessions.getIfPresent(sessionToken))).flatMap{
        case Some(session) if !session.completed =>
          for {
            _ <- IO {
              sessions.invalidate(session.token)
              sessions.put(newToken, session.copy(token = newToken, completed = true))
            }
            _ <- processUploadedFile (session.objectName)
          } yield session
        case _ =>
          IO.raiseError(InvalidInputException(s"不合法的sessionToken"))
      }
      _ <- sendMessage(newToken, session.userID, session.objectName)
    } yield()
  }

  private def processUploadedFile(objectName: String)(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Processing uploaded file $objectName"))
      fileSize <- IO(minioClient.statObject(
        StatObjectArgs.builder()
          .bucket("temp")
          .`object`(objectName)
          .build()
      )).map(stat => stat.size())
        .handleErrorWith(ex =>
          IO(logger.info(s"Object size not found!!")) *>
            IO.raiseError(InvalidInputException(s"查找上传的文件时发生错误：${ex.getMessage}"))
        )
      _ <- IO(logger.info(s"Object size is ${getHumanReadableSize(fileSize)}"))
      _ <- if (fileSize < 10 * 1024) {
        IO.raiseError(InvalidInputException(s"上传的文件过小(${getHumanReadableSize(fileSize)}, 至少需要 10KB)"))
      } else if (fileSize > 5 * 1024 * 1024) {
        IO.raiseError(InvalidInputException(s"上传的文件过大(${getHumanReadableSize(fileSize)}, 最多只能 5MB)"))
      } else IO.unit
      _ <- IO(logger.info(s"Input is valid, moving file"))
      _ <- IO(minioClient.copyObject(
        CopyObjectArgs.builder()
          .bucket("avatar")
          .`object`(objectName)
          .source(
            CopySource.builder()
              .bucket("temp")
              .`object`(objectName)
              .build()
          ).build()
      ))
    } yield()
  }

  private def getHumanReadableSize(fileSize: Long): String = {

    val units = Array("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(fileSize.toDouble) / Math.log10(1024)).toInt
    new DecimalFormat("#,##0.#")
      .format(fileSize / Math.pow(1024, digitGroups)) + " " + units(digitGroups)
  }

  private case class Payload(token: String, id: Int, file_name: String, task: String = "avatar");

  private def sendMessage(token: String, userID: Int, objName: String): IO[Unit] = clientResource.use { client =>
    val request = Request[IO](
      method = Method.POST,
      uri = Uri.unsafeFromString(minioConfig.mediaEndpoint + "/image")
    ).withEntity(Payload(token, userID, objName).asJson.asString.getOrElse(""))

    client.run(request).use { response => response.body.compile.drain }
  }
}