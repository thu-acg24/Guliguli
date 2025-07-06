package DBManager.Process

import Global.DBConfig
import DBManager.Impl.{Routes, SwitchDataSourceMessagePlanner}
import DBManager.Process.SourceUtils.createDataSource
import cats.effect.{ExitCode, IO, Ref, Resource}
import com.comcast.ip4s.*
import com.zaxxer.hikari.HikariDataSource
import org.http4s.*
import org.http4s.ember.server.*
import org.http4s.implicits.*
import org.http4s.server.Router
import org.slf4j.LoggerFactory
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.{Slf4jFactory, Slf4jLogger}

import java.sql.Connection
import scala.collection.mutable
import scala.concurrent.duration.*

object DBServer{
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  given Slf4jFactory[IO] = Slf4jFactory.create[IO]

  private val log = LoggerFactory.getLogger(this.getClass.getSimpleName)

  private val dbHost = "0.0.0.0"
  private val dbPort = 10002
  private val defaultTimeout = 30.minutes



  def setDBServer():IO[Unit] =
    (for {
      // Lift Ref creation into Resource using Resource.eval
      connectionMap <- Resource.eval(Ref.of[IO, Map[String, Connection]](Map.empty))
      dataSourceRef <- Resource.eval(Ref.of[IO, Option[HikariDataSource]](None))
      dbConfig <-Resource.eval(Utils.readConfig("db_config.json"))
      _ = println(s"setDBServer - dbConfig = ${dbConfig}")

      _ <- Resource.eval {
        for {
          tempDS <- createDataSource(dbConfig, "postgres")
          projectNameOpt <- IO {
            val conn = tempDS.getConnection
            val stmt = conn.createStatement()
            stmt.execute(
              """
                |CREATE TABLE IF NOT EXISTS public.projectName (
                |  name TEXT NOT NULL
                |);
                |""".stripMargin)
            val rs = stmt.executeQuery("SELECT name FROM projectName LIMIT 1")
            val result =
              if (rs.next()) Some(rs.getString("name")) else None
            rs.close()
            stmt.close()
            conn.close()
            tempDS.close()
            result
          }

          _ <- projectNameOpt match {
            case Some(projectName) =>
              println(s"Found existing projectName: $projectName, switching data source.")
              SwitchDataSourceMessagePlanner(projectName).plan(dataSourceRef, dbConfig).void
            case None =>
              println("No projectName found. Proceeding without switching data source.")
              IO.unit
          }
        } yield ()
      }


      // Create the server as a Resource
      host = Host.fromString(dbHost).getOrElse(
        throw new IllegalArgumentException("Invalid IPv4 address for db server: 0.0.0.0")
      )
      port = Port.fromInt(dbPort).getOrElse(
        throw new IllegalArgumentException("Invalid port for db server: 10002")
      )
      _ = println(s"setDBServer - host = ${host}, port = ${port}")

      server <- EmberServerBuilder.default[IO]
        .withHost(host)
        .withPort(port)
        .withIdleTimeout(defaultTimeout)
        .withShutdownTimeout(defaultTimeout)
        .withRequestHeaderReceiveTimeout(defaultTimeout)
        .withHttpApp(Router("/" -> Routes.Routes.service(dataSourceRef, connectionMap, dbConfig)).orNotFound)
        .build
    } yield server)
      .use(_ => IO.never)
      .handleErrorWith { e =>
        IO(println(s"Server error: ${e.getMessage}")).as(ExitCode.Error) // Log the error and return an error exit code
      }.start.void
}
