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
import cats.effect.std.Random
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.GetPresignedObjectUrlArgs
import io.minio.http.Method
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import scala.jdk.CollectionConverters._
import java.util.concurrent.TimeUnit

//process plan import 预留标志位，不要删除

case object MediaProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除

  // implicit val dateTimeDecoder: Decoder[DateTime] = decodeDateTime



  def validateVideo(videoID: Int)(using PlanContext): IO[Int] = {
    val sql = s"SELECT uploader_id FROM ${schemaName}.video_table WHERE video_id = ?;"
    val param = List(SqlParameter("Int", videoID.toString))
    readDBJsonOptional(sql, param).map {
      case None => throw InvalidInputException("视频不存在")
      case Some(json) => decodeField[Int](json, "uploader_id")
    }
  }

  def updateVideoStatus(videoID: Int)(using PlanContext): IO[String] = {
    IO(logger.info(s"[updateVideoStatus] Updating video status for videoID=${videoID} to status=Uploading")) >> {
      val updateSql = s"UPDATE ${schemaName}.video_table SET status = 'Uploading' WHERE video_id = ?;"
      writeDB(updateSql, List(
        SqlParameter("Int", videoID.toString)
      ))
    }
  }

  def generateObjectName(userID: Int, info: String): IO[String] = {
    for {
      timestamp <- IO.realTimeInstant.map(_.toEpochMilli)
      random <- Random.scalaUtilRandom[IO].flatMap(_.betweenInt(0, 10000))
    } yield s"$userID/$timestamp-$info-$random"
  }

  def generateUploadUrl(objectName: String, extraParams: java.util.Map[String, String] = Map().asJava): IO[String] = {
    IO.blocking { // 包装阻塞IO操作
      minioClient.getPresignedObjectUrl(
        io.minio.GetPresignedObjectUrlArgs.builder()
          .method(Method.PUT)
          .bucket("temp")
          .`object`(objectName)
          .expiry(30, TimeUnit.MINUTES)
          .extraQueryParams(extraParams)
          .build()
      )
    }
  }
}