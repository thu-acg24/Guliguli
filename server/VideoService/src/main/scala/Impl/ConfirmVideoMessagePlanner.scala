package Impl

import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.sessions
import Objects.UploadSession
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.{CopyObjectArgs, CopySource, StatObjectArgs}
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ConfirmVideoMessagePlanner(
                                         sessionToken: String,
                                         status: String,
                                         m3u8Name: String,
                                         tsPrefix: String,
                                         sliceCount: Int,
                                         duration: Float,
                                         override val planContext: PlanContext
                                       ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan definition
  override def plan(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Validating token $sessionToken for video"))
      session <- IO(Option(sessions.getIfPresent(sessionToken))).flatMap {
        case Some(session) if session.completed =>
          IO.pure(session)
        case _ =>
          IO.raiseError(InvalidInputException(s"不合法的sessionToken"))
      }
      _ <- IO(sessions.invalidate(session.token))
      _ <- status match {
        case "success" => updateVideoInDB(session.videoID, sliceCount)
        case "failure" => updateVideoInDB(session.videoID, 0)
        case _ => IO.raiseError(InvalidInputException(s"status必须是success或failure中的一个"))
      }
    } yield ()
  }

  private def updateVideoInDB(videoID: Int, sliceCount: Int)(using PlanContext): IO[Unit] = {
    val querySQL =
      s"""
           UPDATE ${schemaName}.video_table
           SET m3u8_name = ?, ts_prefix = ?, slice_count = ?, duration = ?
           WHERE video_id = ?
         """.stripMargin

    val queryParams = List(
      SqlParameter("String", m3u8Name),
      SqlParameter("String", tsPrefix),
      SqlParameter("Int", sliceCount.toString),
      SqlParameter("Float", duration.toString),
      SqlParameter("Int", videoID.toString)
    )

    for {
      _ <- IO(logger.info(s"Executing update query: $querySQL with params: $queryParams"))
      updateResponse <- writeDB(querySQL, queryParams)
    } yield ()
  }
}