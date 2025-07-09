package Impl

import APIs.RecommendationService.RecordWatchDataMessage
import APIs.HistoryService.AddBrowseHistoryMessage
import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.{m3u8Cache, minioClient, readLines}
import Utils.VideoAuth.{validateToken, validateVideoID}
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.minio.{GetPresignedObjectUrlArgs, PutObjectArgs}
import io.minio.http.Method
import org.slf4j.LoggerFactory

import java.io.ByteArrayInputStream
import java.util.UUID
import scala.util.matching.Regex

case class QueryM3U8PathMessagePlanner(
    token: Option[String],
    videoID: Int,
    override val planContext: PlanContext
) extends Planner[String] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate token to ensure user access authorization
      user <- validateToken(token)
      // Step 2: Validate if the video ID is valid and accessible
      _ <- validateVideoID(videoID, user)
      // Step 3: Query video information if validation passes
      (m3u8Name, tsPrefix, sliceCount) <- queryVideoPath()
      cached <- IO.blocking(Option(m3u8Cache.getIfPresent(m3u8Name)))
      finalUrl <- cached match {
        case Some(url) => IO.pure(url)
        case None =>
          for {
            // Step 4: Read template m3u8
            templateLines <- readLines(bucket, m3u8Name)
            // Step 5: Generate ts filenames
            tsKeys = generateTsKeys(tsPrefix, sliceCount)
            // Step 6: Generate signed URLs
            tsUrls <- generatePresignedUrls(tsKeys)
            // Step 7: Replace placeholders
            replacedLines = replaceSegments(templateLines, tsUrls)
            newM3U8Content = replacedLines.mkString("\n")
            // Step 8: Upload and get final share link
            finalUrl <- uploadNewM3U8(newM3U8Content, videoID.toString + "/" + UUID.randomUUID().toString + ".m3u8")
            _ <- IO.blocking(m3u8Cache.put(m3u8Name, finalUrl))
          } yield finalUrl
      }
      _ <- token.map(updateViewInfo(_, videoID)).getOrElse(IO.unit)
    } yield finalUrl
  }

  private def queryVideoPath()(using PlanContext): IO[(String, String, Int)] = {
    for {
      _ <- IO(logger.info(s"[Step 3] Querying detailed information for videoID: $videoID"))
      videoQueryResult <- readDBJsonOptional(
        s"""
          SELECT m3u8_name, ts_prefix, slice_count
          FROM $schemaName.video_table
          WHERE video_id = ?;
        """.stripMargin,
        List(SqlParameter("Int", videoID.toString))
      )

      result <- videoQueryResult match {
        case Some(json) =>
          IO(logger.info(s"[Step 3.1] Video information found, json: $json")) >>
            IO((decodeField[String](json, "m3u8_name"),
              decodeField[String](json, "ts_prefix"), decodeField[Int](json, "slice_count")))
        case None =>
          IO(logger.info("[Step 3.1] No video details found in the database")) >>
            IO.raiseError(InvalidInputException("Video does not exist"))
      }
    } yield result
  }

  // 生成形如 tsPrefix_00000.ts 的 ts 文件名
  private def generateTsKeys(tsPrefix: String, sliceCount: Int): List[String] = {
    (0 until sliceCount).map(i => f"${tsPrefix}_$i%05d.ts").toList
  }

  private val bucket = "video-server"

  // 为每个 ts 文件生成签名 URL
  private def generatePresignedUrls(tsKeys: List[String]): IO[List[String]] = IO.parTraverseN(10)(tsKeys) { key =>
      IO{minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
          .method(Method.GET)
          .bucket(bucket)
          .`object`(key)
          .expiry(7 * 24 * 60 * 60) // 一周有效
          .build()
      )}
  }

  // 替换 m3u8 模板文件中的 segment_[number].ts 占位符
  private def replaceSegments(templateLines: List[String], tsUrls: List[String]): List[String] = {
    val tsPlaceholderPattern: Regex = raw"segment_(\d{5})\.ts".r

    templateLines.map { line =>
      tsPlaceholderPattern.findFirstMatchIn(line) match {
        case Some(m) =>
          val index = m.group(1).toInt  // 提取五位数字并转为整数
          if (index < tsUrls.length) {
            tsPlaceholderPattern.replaceFirstIn(line, tsUrls(index))
          } else {
            throw InvalidInputException("索引不匹配")  // 索引越界时保留原行（根据要求可不处理）
          }
        case None => line  // 无匹配时保留原行
      }
    }
  }

    // 上传新的 m3u8 内容并生成分享链接
  private def uploadNewM3U8(content: String, targetKey: String): IO[String] = {
    val inputStream = new ByteArrayInputStream(content.getBytes("UTF-8"))
    IO.fromCompletableFuture(IO(minioClient.putObject(
      PutObjectArgs.builder()
        .bucket(bucket)
        .`object`(targetKey)
        .stream(inputStream, content.length, -1)
        .contentType("application/vnd.apple.mpegurl")
        .build()
    ))) >> IO.blocking {
      minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
          .method(Method.GET)
          .bucket(bucket)
          .`object`(targetKey)
          .expiry(7 * 24 * 60 * 60)
          .build()
      )
    }
  }

  private def updateViewInfo(token: String, videoID: Int)(using PlanContext): IO[Unit] = {
    for {
      _ <- AddBrowseHistoryMessage(token, videoID).send
      recordable <- RecordWatchDataMessage(token, videoID).send
      _ <- if (recordable) writeDB(
        s"""
           |UPDATE $schemaName.video_table
           |SET views = views + 1
           |WHERE video_id = ?
           |""".stripMargin, List(
          SqlParameter("Int", videoID.toString)
        )).void else IO.unit
    } yield ()
  }
}