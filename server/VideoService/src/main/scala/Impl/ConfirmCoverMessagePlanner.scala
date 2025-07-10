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
import Utils.VerifyProcess.checkVideoStatus

case class ConfirmCoverMessagePlanner(
                                         sessionToken: String,
                                         status: String,
                                         objectName: String,
                                         override val planContext: PlanContext
                                       ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan definition
  override def plan(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Validating token $sessionToken for cover"))
      session <- IO(Option(sessions.getIfPresent(sessionToken))).flatMap{
        case Some(session) if session.completed =>
          IO.pure(session)
        case _ =>
          IO.raiseError(InvalidInputException(s"不合法的sessionToken"))
      }
      _ <- IO(sessions.invalidate(session.token))
      _ <- status match {
        case "success" => updateCoverLinkInDB(session.token, session.videoID, objectName)
        case "failure" => failureControl(session.videoID)
        case _ => IO.raiseError(InvalidInputException(s"status必须是success或failure中的一个"))
      }
    } yield()
  }

  private def updateCoverLinkInDB(token: String, videoID: Int, objectName: String)(using PlanContext): IO[Unit] = {
    val querySQL =
      s"""
           UPDATE $schemaName.video_table
           SET cover = ?
           WHERE video_id = ?
         """.stripMargin

    val queryParams = List(
      SqlParameter("String", objectName),
      SqlParameter("Int", videoID.toString)
    )

    for {
      _ <- IO(logger.info(s"Executing update query: $querySQL with params: $queryParams"))
      updateResponse <- writeDB(querySQL, queryParams)
      _ <- checkVideoStatus(token, videoID)
    } yield ()
  }
  private def failureControl(videoID: Int)(using PlanContext): IO[Unit] = {
    val querySQL =
      s"""
               UPDATE $schemaName.video_table
               SET cover = ?, status = ?
               WHERE video_id = ?
             """.stripMargin

    val queryParams = List(
      SqlParameter("String", "0/fallback/image_fallback.jpg"),
      SqlParameter("String", "Broken"),
      SqlParameter("Int", videoID.toString)
    )

    for {
      _ <- IO(logger.info(s"Executing update query: $querySQL with params: $queryParams"))
      updateResponse <- writeDB(querySQL, queryParams)
    } yield ()
  }

}