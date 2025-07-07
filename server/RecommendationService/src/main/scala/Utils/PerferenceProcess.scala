package Utils

import Common.API.PlanContext
import Common.DBAPI.{decodeField, readDBJson, readDBJsonOptional, writeDB}
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.implicits.*
import Objects.PGVector
import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto.*
import org.slf4j.LoggerFactory
import java.util.UUID

//process plan import 预留标志位，不要删除

case object PerferenceProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除

  def getInfo(strings: List[String])(using PlanContext): IO[PGVector] = {
    for {
      initVector <- PGVector.fromString(UUID.randomUUID().toString)
      result <- strings.traverse(str => PGVector.fromString(str)).map { vectors =>
        vectors.foldLeft(initVector)(_ + _)
      }
    } yield result.normalize
  }

  def updateEmbedding(userID: Int, videoID: Int, userRatio: Option[Float], videoRatio: Option[Float])(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"开始获取视频和用户的embedding: userID=$userID, videoID=$videoID"))
      videoVector <- getVideoVector(videoID)
      _ <- IO(logger.info(s"视频videoID=$videoID 的embedding为${videoVector.toString}"))
      userVector <- getUserVector(userID)
      _ <- IO(logger.info(s"视频videoID=$userID 的embedding为${userVector.toString}"))
      _ <- userRatio.map(ratio => writeDB(
        s"""
           |UPDATE ${schemaName}.video_info_table
           |SET embedding = ?
           |WHERE video_id = ?
           |""".stripMargin, List(
          SqlParameter("Vector", (videoVector + userVector * ratio).normalize.toString),
          SqlParameter("Int", videoID.toString)
        ))).getOrElse(IO.pure(""))
      _ <- videoRatio.map(ratio => writeDB(
        s"""
           |UPDATE ${schemaName}.user_info_table
           |SET embedding = ?
           |WHERE user_id = ?
           |""".stripMargin, List(
          SqlParameter("Vector", (userVector + videoVector * ratio).normalize.toString),
          SqlParameter("Int", userID.toString)
        ))).getOrElse(IO.pure(""))
    } yield()
  }

  def getVideoVector(videoID: Int)(using PlanContext): IO[PGVector] = {
    readDBJson(
      s"""
         |SELECT embedding
         |FROM ${schemaName}.video_info_table
         |WHERE video_id = ?
         |""".stripMargin, List(
        SqlParameter("Int", videoID.toString)
      )).map(json => decodeField[PGVector](json, "embedding"))
  }

  def getUserVector(userID: Int)(using PlanContext): IO[PGVector] = {
    readDBJsonOptional(
      s"""
         |SELECT embedding
         |FROM ${schemaName}.user_info_table
         |WHERE user_id = ?
         |""".stripMargin, List(
        SqlParameter("Int", userID.toString)
      )).flatMap {
      case Some(json) => IO(decodeField[PGVector](json, "embedding"))
      case None => initUserEmbedding(userID)
    }
  }

  private def initUserEmbedding(userID: Int)(using PlanContext): IO[PGVector] = {
    for {
      _ <- IO(logger.info(s"正在初始化用户userID=$userID 的Embedding"))
      initVector <- PGVector.fromString(UUID.randomUUID().toString)
      _ <- writeDB(
        s"""
           |INSERT INTO ${schemaName}.user_info_table
           |(user_id, embedding)
           |VALUES (?, ?)
           |""".stripMargin, List(
          SqlParameter("Int", userID.toString),
          SqlParameter("Vector", initVector.toString)
        ))
    } yield initVector
  }
}
