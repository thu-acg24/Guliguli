package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ModifyVideoMessagePlanner(
                                      token: String,
                                      videoID: Int,
                                      videoPath: Option[String],
                                      title: Option[String],
                                      coverPath: Option[String],
                                      description: Option[String],
                                      tag: List[String],
                                      duration: Option[Int],
                                      override val planContext: PlanContext
                                    ) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"Start ModifyVideoMessagePlanner with token: ${token}, videoID: ${videoID}"))

      // Step 1: Validate token
      userIDOpt <- validateToken()
      resultOpt <- userIDOpt match {
        case None => IO(Some("Invalid Token"))
        case Some(userID) =>
          for {
            // Step 2: Check if video record exists
            videoOpt <- getVideoById(videoID)
            res <- videoOpt match {
              case None => IO(Some("Video Not Found"))
              case Some(video) =>
                for {
                  // Step 3: Check upload permissions
                  permissionRes <- validateUploaderPermission(userID, video)
                  finalRes <- permissionRes match {
                    case Some(msg) => IO(Some(msg))
                    case None => 
                      // Step 4: Update video fields
                      updateVideo(videoID, videoPath, title, coverPath, description, tag, duration)
                  }
                } yield finalRes
            }
          } yield res
      }
    } yield resultOpt
  }

  private def validateToken()(using PlanContext): IO[Option[Int]] = {
    IO(logger.info(s"Validating token: ${token}")) >>
    GetUIDByTokenMessage(token).send
  }

  private def getVideoById(videoID: Int)(using PlanContext): IO[Option[Json]] = {
    val sql =
      s"""
         SELECT *
         FROM ${schemaName}.video_table
         WHERE video_id = ?;
       """
    for {
      _ <- IO(logger.info(s"Fetching video record for videoID: ${videoID}"))
      result <- readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString)))
    } yield result
  }

  private def validateUploaderPermission(userID: Int, video: Json)(using PlanContext): IO[Option[String]] = {
    val uploaderID = decodeField[Int](video, "uploader_id")
    val result = if (userID != uploaderID) Some("Permission Denied") else None
    IO(logger.info(s"Validating uploader permission for userID: ${userID}, uploaderID: ${uploaderID}, result: ${result}")) >>
    IO(result)
  }

  private def updateVideo(
                           videoID: Int,
                           videoPath: Option[String],
                           title: Option[String],
                           coverPath: Option[String],
                           description: Option[String],
                           tag: List[String],
                           duration: Option[Int]
                         )(using PlanContext): IO[Option[String]] = {
    val updates = List(
      videoPath.map { path => "server_path = ?" -> SqlParameter("String", path) },
      title.map { t => "title = ?" -> SqlParameter("String", t) },
      coverPath.map { path => "cover_path = ?" -> SqlParameter("String", path) },
      description.map { desc => "description = ?" -> SqlParameter("String", desc) },
      if (tag.nonEmpty) Some("tag = ?" -> SqlParameter("Array[String]", tag.asJson.noSpaces)) else None,
      duration.map { d => "duration = ?" -> SqlParameter("Int", d.toString) },
      Some("status = ?" -> SqlParameter("String", "Pending")),
      Some("last_modified_time = ?" -> SqlParameter("DateTime", DateTime.now.getMillis.toString))
    ).flatten

    if (updates.isEmpty) {
      IO(logger.info(s"No fields to update for videoID: ${videoID}")) >>
      IO(None)
    } else {
      val (setClause, sqlParams) = updates.unzip
      val sql =
        s"""
           UPDATE ${schemaName}.video_table
           SET ${setClause.mkString(", ")}
           WHERE video_id = ?;
         """
      val parameters = sqlParams :+ SqlParameter("Int", videoID.toString)

      for {
        _ <- IO(logger.info(s"Executing SQL: ${sql} with parameters: ${parameters}"))
        result <- writeDB(sql, parameters).map(_ => None).handleErrorWith { ex =>
          IO(logger.error(s"Failed to update video information for videoID: ${videoID}, error: ${ex}")) >>
          IO(Some("Failed to modify video information"))
        }
      } yield result
    }
  }
}