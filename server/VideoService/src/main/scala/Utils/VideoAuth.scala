package Utils

import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.minioClient
import Objects.VideoService.{Video, VideoInfo, VideoStatus}
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.GetPresignedObjectUrlArgs
import io.minio.http.Method
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit

//process plan import 预留标志位，不要删除

case object VideoAuth {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除

  // implicit val dateTimeDecoder: Decoder[DateTime] = decodeDateTime

  def decodeVideo(json: Json)(using PlanContext): IO[Video] = {
    for {
      _ <- IO(logger.info(s"Given json: $json"))
      video <- IO(decodeType[Video](json))
      cover <- IO(video.cover.map(
        file_name => minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
          .bucket("video-cover")  // 你的封面 bucket 名称
          .`object`(file_name)  // 封面文件名
          .expiry(6, TimeUnit.HOURS)  // 6小时有效期
          .method(Method.GET)  // GET 请求
          .build())))
      result <- IO(video.copy(cover = cover))
    } yield result
  }

  def decodeVideoInfo(json: Json)(using PlanContext): IO[VideoInfo] = {
    for {
      _ <- IO(logger.info(s"Given json: $json"))
      video <- IO(decodeType[VideoInfo](json))
      cover <- IO(video.cover.map(
        file_name => minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
            .bucket("video-cover")
            .`object`(file_name)
            .expiry(6, TimeUnit.HOURS)
            .method(Method.GET)
            .build())))
      m3u8 <- IO(video.m3u8Path.map(
        file_name => minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
        .bucket("video-server")
        .`object`(file_name)
        .expiry(6, TimeUnit.HOURS)
        .method(Method.GET)
        .build())))
      ts <- (video.tsPrefix, video.sliceCount) match {
        case (Some(prefix), Some(count)) if count > 0 =>
          val tsFiles = (0 until count).map
            ( i => f"$count$i%05d.ts" )
          tsFiles.toList.parTraverse { tsFile =>
            IO.blocking {
              minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                  .bucket("video-server")
                  .`object`(tsFile)
                  .expiry(6, TimeUnit.HOURS)
                  .method(Method.GET)
                  .build()
              )
            }
          }.map { urls =>
            Some(urls.filter(_.nonEmpty))  // 过滤掉无效的 URL
          }
        case _ => IO.pure(None)
      }
      result <- IO(video.copy(cover = cover, m3u8Path = m3u8, tsPath = ts))
    } yield result
  }
}