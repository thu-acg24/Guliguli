
package Process


import Common.API.PlanContext
import Common.API.TraceID
import Common.APIException.InvalidInputException
import Common.DBAPI.DidRollbackException
import Common.Serialize.CustomColumnTypes.*
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Impl.AddVideoInfoMessagePlanner
import Impl.DeleteVideoInfoMessagePlanner
import Impl.GetRecommendedVideosMessagePlanner
import Impl.RecordWatchDataMessagePlanner
import Impl.SearchVideosMessagePlanner
import Impl.UpdateFeedbackFavoriteMessagePlanner
import Impl.UpdateFeedbackLikeMessagePlanner
import Impl.UpdateVideoInfoMessagePlanner
import cats.effect.*
import fs2.concurrent.Topic
import io.circe.*
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*

import java.util.UUID
import org.http4s.*
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.dsl.io.*
import org.joda.time.DateTime

import scala.collection.concurrent.TrieMap

object Routes:
  val projects: TrieMap[String, Topic[IO, String]] = TrieMap.empty

  private def executePlan(messageType: String, str: String): IO[String] =
    messageType match {
      case "AddVideoInfoMessage" =>
        IO(
          decode[AddVideoInfoMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for addVideoInfoMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten

      case "DeleteVideoInfoMessage" =>
        IO(
          decode[DeleteVideoInfoMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for deleteVideoInfoMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten

      case "GetRecommendedVideosMessage" =>
        IO(
          decode[GetRecommendedVideosMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for GetRecommendedVideosMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten

      case "RecordWatchDataMessage" =>
        IO(
          decode[RecordWatchDataMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for RecordWatchDataMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "SearchVideosMessage" =>
        IO(
          decode[SearchVideosMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for SearchVideosMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "UpdateFeedbackLikeMessage" =>
        IO(
          decode[UpdateFeedbackLikeMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for UpdateFeedbackLikeMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten

      case "UpdateFeedbackFavoriteMessage" =>
        IO(
          decode[UpdateFeedbackFavoriteMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for UpdateFeedbackFavoriteMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten

      case "UpdateVideoInfoMessage" =>
        IO(
          decode[UpdateVideoInfoMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for updateVideoInfoMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       

      case "test" =>
        for {
          output  <- Utils.Test.test(str)(using  PlanContext(TraceID(""), 0))
        } yield output
      case _ =>
        IO.raiseError(new Exception(s"Unknown type: $messageType"))
    }

  def handlePostRequest(req: Request[IO]): IO[String] = {
    req.as[Json].map { bodyJson =>
      val hasPlanContext = bodyJson.hcursor.downField("planContext").succeeded

      val updatedJson = if (hasPlanContext) {
        bodyJson
      } else {
        val planContext = PlanContext(TraceID(UUID.randomUUID().toString), transactionLevel = 0)
        val planContextJson = planContext.asJson
        bodyJson.deepMerge(Json.obj("planContext" -> planContextJson))
      }
      updatedJson.toString
    }
  }
  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "health" =>
      Ok("OK")
      
    case GET -> Root / "stream" / projectName =>
      projects.get(projectName) match {
        case Some(topic) =>
          val stream = topic.subscribe(10)
          Ok(stream)
        case None =>
          Topic[IO, String].flatMap { topic =>
            projects.putIfAbsent(projectName, topic) match {
              case None =>
                val stream = topic.subscribe(10)
                Ok(stream)
              case Some(existingTopic) =>
                val stream = existingTopic.subscribe(10)
                Ok(stream)
            }
          }
      }
    case req@POST -> Root / "api" / name =>
      handlePostRequest(req).flatMap {
        executePlan(name, _)
      }.flatMap(Ok(_))
      .handleErrorWith {
        case e: DidRollbackException =>
          println(s"Rollback error: $e")
          val headers = Headers("X-DidRollback" -> "true")
          BadRequest(e.getMessage.asJson.toString).map(_.withHeaders(headers))

        case e: InvalidInputException =>
          val headers = Headers("X-InvalidInput" -> "true")
          BadRequest(e.getMessage).map(_.withHeaders(headers))

        case e: Throwable =>
          println(s"General error: $e")
          BadRequest(e.getMessage.asJson.toString)
      }
  }
  