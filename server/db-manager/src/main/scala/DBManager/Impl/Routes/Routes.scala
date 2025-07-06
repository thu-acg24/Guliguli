package DBManager.Impl.Routes

import Common.API.{PlanContext, TraceID}
import Common.DBAPI.DidRollbackException
import DBManager.*
import DBManager.Impl.*
import Global.DBConfig
import cats.effect.*
import com.zaxxer.hikari.HikariDataSource
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.http4s.*
import org.http4s.dsl.io.*

import java.sql.{Connection, SQLException}
import scala.concurrent.duration.*


object Routes:
  private def withConnectionMap[T: Encoder](message: DBPlanner[T], dataSource: HikariDataSource, connectionMap: Ref[IO, Map[String, Connection]]): IO[String] =

    /** 只有是在transaction的情况下，我们才需要保存connection，否则是不需要的！ */
    if (message.planContext.transactionLevel > 0)
      connectionMap.get.flatMap { map =>
        map.get(message.planContext.traceID.id) match {
          case Some(existingConnection) =>
            // Use the existing connection from the map
            message.planWithConnection(existingConnection, connectionMap).map(implicitly[JsonStringEncoder[T]].toJsonString).handleErrorWith {
              case error: SQLException if error.getMessage.contains("Connection is closed") =>
                connectionMap.update(_ - message.planContext.traceID.id) >> withConnectionMap(message, dataSource, connectionMap) // update connection map and redo the operation.
              case other => IO.raiseError(other) // Rethrow any other exception
            }
          case None =>
            for {
              newConnection <- IO(dataSource.getConnection)
              _ <- connectionMap.update(_ + (message.planContext.traceID.id -> newConnection))
              result <- message.planWithConnection(newConnection, connectionMap).map(implicitly[JsonStringEncoder[T]].toJsonString)
              _ <- (for {
                _ <- IO.sleep(5.minutes)
                _ <- connectionMap.update(_ - message.planContext.traceID.id)
                _ <- IO(newConnection.close())
              } yield ()).start // Run the cleanup process asynchronously
            } yield result
        }
      }
    else
      Resource.make(
        IO(dataSource.getConnection)
      ) { connection =>
        IO(connection.close())
      }.use {
        newConnection =>
          message.planWithConnection(newConnection, connectionMap).map(implicitly[JsonStringEncoder[T]].toJsonString).flatMap {
            a => IO.println(a) >> IO.pure(a)
          }
      }

  private def executePlan(messageType: String, str: String, dataSourceRef: Ref[IO, Option[HikariDataSource]], connectionMap: Ref[IO, Map[String, Connection]]): IO[String] = {
    // Check the current state of dataSourceRef
    IO.println(s"messageType = ${messageType}, str = ${str}")

    dataSourceRef.get.flatMap {
      case None =>
        IO.raiseError(new Exception("No active datasource. Please create a new datasource first."))
      case Some(dataSource) =>
        IO.println(str) >>
          (messageType match {
            case "InitSchemaMessage" =>
              IO.fromEither(decode[InitSchemaMessagePlanner](str)).flatMap(msg => withConnectionMap(msg, dataSource, connectionMap))
            case "EndTransactionMessage" =>
              IO.fromEither(decode[EndTransactionMessagePlanner](str)).flatMap(msg => withConnectionMap(msg, dataSource, connectionMap))
            case "ReadDBRowsMessage" =>
              IO.fromEither(decode[ReadDBRowsMessagePlanner](str)).flatMap(msg => withConnectionMap[List[Json]](msg, dataSource, connectionMap))
            case "ReadDBValueMessage" =>
              IO.fromEither(decode[ReadDBValueMessagePlanner](str)).flatMap(msg => withConnectionMap(msg, dataSource, connectionMap))
            case "WriteDBMessage" =>
              IO.fromEither(decode[WriteDBMessagePlanner](str)).flatMap(msg => withConnectionMap(msg, dataSource, connectionMap))
            case "WriteDBListMessage" =>
              IO.fromEither(decode[WriteDBListMessagePlanner](str)).flatMap(msg => withConnectionMap(msg, dataSource, connectionMap))
            case "StartTransactionMessage" =>
              IO.fromEither(decode[StartTransactionMessagePlanner](str)).flatMap(msg => withConnectionMap(msg, dataSource, connectionMap))
            case _ =>
              IO.raiseError(new Exception(s"Unknown type: $messageType"))
          })
    }

}


  def service(dataSourceRef: Ref[IO, Option[HikariDataSource]], connectionMap: Ref[IO, Map[String, Connection]], dbConfig: DBConfig): HttpRoutes[IO] = HttpRoutes.of[IO]:
    case GET -> Root / "health" =>
      Ok("OK")
    case req@POST -> Root / "api" / name =>
      req.as[String].flatMap {
          str =>
            if (name == "SwitchDataSourceMessage")
              IO.fromEither(decode[SwitchDataSourceMessagePlanner](str)).flatMap(_.plan(dataSourceRef,dbConfig).map(a=> Json.fromString(a).noSpaces))
            else
              executePlan(name, str, dataSourceRef, connectionMap)
        }.flatMap {
        a=>
          println(a)
          Ok(a)
        }
        .handleErrorWith {
          case e: DidRollbackException =>
            println(s"Rollback error: $e")
            val headers = Headers("X-DidRollback" -> "true")
            BadRequest(e.getMessage.asJson.toString).map(_.withHeaders(headers))
          case e: Throwable =>
            println(s"General error: $e")
            e.printStackTrace()
            BadRequest(e.getMessage.asJson.toString)
        }
