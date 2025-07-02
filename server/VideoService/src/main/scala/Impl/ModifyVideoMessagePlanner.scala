package Impl


import APIs.UserService.{GetUIDByTokenMessage, QueryUserRoleMessage}
import Common.API.PlanContext
import Common.APIException.InvalidInputException
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import cats.effect.IO
import cats.implicits.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ModifyVideoMessagePlanner(
                                      token: String,
                                      videoID: Int,
                                      videoPath: Option[String],
                                      title: Option[String],
                                      coverPath: Option[String],
                                      description: Option[String],
                                      tag: Option[List[String]],
                                      duration: Option[Int],
                                      override val planContext: PlanContext
                                    ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Start ModifyVideoMessagePlanner with token: ${token}, videoID: ${videoID}"))

      // Step 1: Validate token
      userID <- validateToken()

      // Step 2: Validate video existence and fetch uploader ID
      _ <- IO(logger.info(s"开始校验视频ID是否合法, videoID=${videoID}"))
      uploaderID <- fetchUploaderID(videoID)
      _ <- IO(logger.info(s"获取到视频的上传者ID: ${uploaderID}"))

      // Step 3: Check user permissions
      _ <- IO(logger.info(s"开始校验用户是否有权限修改该视频, userID=${userID}, uploaderID=${uploaderID}"))
      hasPermission <- IO(userID == uploaderID)
      _ <- IO(logger.info(s"权限校验结果: ${hasPermission}"))
      _ <- IO.raiseUnless(hasPermission)(InvalidInputException("Permission Denied"))

      // Step 4: Update video fields
      _ <- updateVideo(videoID, videoPath, title, coverPath, description, tag, duration)
    } yield ()
  }

  private def validateToken()(using PlanContext): IO[Int] = {
    IO(logger.info(s"Validating token: ${token}")) >>
    GetUIDByTokenMessage(token).send
  }

  /**
   * 校验视频ID是否存在，并获取上传者ID
   */
  private def fetchUploaderID(videoID: Int)(using PlanContext): IO[Int] = {
    val sql =
      s"""
        SELECT uploader_id
        FROM ${schemaName}.video_table
        WHERE video_id = ?;
      """
    readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).map {
      case None => throw InvalidInputException("视频不存在")
      case Some(json) => decodeField[Int](json, "uploader_id")
    }
  }

  private def updateVideo(
                           videoID: Int,
                           videoPath: Option[String],
                           title: Option[String],
                           coverPath: Option[String],
                           description: Option[String],
                           tag: Option[List[String]],
                           duration: Option[Int]
                         )(using PlanContext): IO[String] = {
    for {
      currentTime <- IO(DateTime.now().getMillis.toString)
      updates <- IO {
        List(
          videoPath.map { path => "server_path = ?" -> SqlParameter("String", path) },
          title.map { t => "title = ?" -> SqlParameter("String", t) },
          coverPath.map { path => "cover_path = ?" -> SqlParameter("String", path) },
          description.map { desc => "description = ?" -> SqlParameter("String", desc) },
          tag.map {t => "tag = ?" -> SqlParameter("Array[String]", t.asJson.noSpaces)},
          duration.map { d => "duration = ?" -> SqlParameter("Int", d.toString) },
          Some("status = ?" -> SqlParameter("String", "Pending")),
          Some("upload_time = ?" -> SqlParameter("DateTime", currentTime))
        ).flatten
      }
      
      result <- if (updates.isEmpty) {
        IO(logger.info(s"No fields to update for videoID: ${videoID}")) >>
        IO("")
      } else {
        val (setClause, sqlParams) = updates.unzip
        val sql =
          s"""
             UPDATE ${schemaName}.video_table
             SET ${setClause.mkString(", ")}
             WHERE video_id = ?;
           """
        val parameters = sqlParams :+ SqlParameter("Int", videoID.toString)
        writeDB(sql, parameters)
      }
    } yield result
  }
}