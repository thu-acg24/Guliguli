package Utils

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
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.StatObjectArgs
import org.http4s.headers.`Content-Type`
import org.http4s.{MediaType, Method, Request, Uri}
import org.http4s.circe.jsonEncoder
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import java.text.DecimalFormat
import java.util.UUID
import java.util.concurrent.TimeUnit

//process plan import 预留标志位，不要删除

case object VerifyProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除

  // implicit val dateTimeDecoder: Decoder[DateTime] = decodeDateTime


  def processUploadedFile(isVideo: Boolean, objectName: String)(using PlanContext): IO[Unit] = {
    val fileLim = if isVideo then 2L * 1024 * 1024 * 1024 else 5L * 1024 * 1024
    for {
      _ <- IO(logger.info(s"Processing uploaded file $objectName"))
      fileSize <- IO.fromCompletableFuture(
          IO.delay {
            minioClient.statObject(
              StatObjectArgs.builder()
                .bucket("temp")
                .`object`(objectName)
                .build()
            )
          }
        ).map(stat => stat.size())
        .handleErrorWith(ex =>
          IO(logger.info(s"Object size not found!!")) *>
            IO.raiseError(InvalidInputException(s"查找上传的文件时发生错误：${ex.getMessage}"))
        )
      _ <- IO(logger.info(s"Object size is ${getHumanReadableSize(fileSize)}"))
      _ <- if (fileSize < 10 * 1024) {
        IO.raiseError(InvalidInputException(s"上传的文件过小(${getHumanReadableSize(fileSize)}), 至少需要 10KB)"))
      } else if (fileSize > fileLim) {
        IO.raiseError(InvalidInputException(s"上传的文件过大(${getHumanReadableSize(fileSize)}), 最多只能 ${getHumanReadableSize(fileLim)})"))
      } else IO.unit
    } yield ()
  }

  def getHumanReadableSize(fileSize: Long): String = {

    val units = Array("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(fileSize.toDouble) / Math.log10(1024)).toInt
    new DecimalFormat("#,##0.#")
      .format(fileSize / Math.pow(1024, digitGroups)) + " " + units(digitGroups)
  }

  def sendMessage(isVideo: Boolean, token: String, videoID: Int, objName: String): IO[Unit] = clientResource.use { client =>
    val request = if (isVideo) {
      Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(minioConfig.mediaEndpoint + "/video")
      ).withEntity(VideoPayload(id = videoID, token = token, file_name = objName).asJson)
    } else {
      Request[IO](
        method = Method.POST,
        uri = Uri.unsafeFromString(minioConfig.mediaEndpoint + "/image")
      ).withEntity(CoverPayload(id = videoID, token = token, file_name = objName).asJson)
    }
    logger.info(s"Sending Message to Media")
    client.run(request).use { response =>
      if (response.status.isSuccess) {
        response.body.compile.drain
      }
      else {
        response.bodyText.compile.string.flatMap { errorBody =>
          IO(sessions.invalidate(token)) >> IO.raiseError(new RuntimeException(
            s"文件处理服务器发生错误 ${response.status.code}: $errorBody"
          ))
        }
      }
    }
  }
  def checkVideoStatus(videoID: Int)(using PlanContext): IO[String] = {
    val querySQL =
      s"""
         |UPDATE $schemaName.video_table
         |SET status = 'Pending'
         |WHERE video_id = ?
         |  AND m3u8_name IS NOT NULL
         |  AND m3u8_name NOT LIKE '0/%'
         |  AND cover IS NOT NULL
         |  AND cover NOT LIKE '0/%'
       """.stripMargin
    writeDB(querySQL, List(SqlParameter("Int", videoID.toString)))
  }
}