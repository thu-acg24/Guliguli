package Process

import Common.API.{PlanContext, TraceID}
import Process.SourceUtils.createDataSource
import cats.effect.*
import com.comcast.ip4s.*
import org.http4s.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.*
import org.http4s.implicits.*
import org.http4s.server.middleware.CORS
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.{Slf4jFactory, Slf4jLogger}

import java.nio.channels.ClosedChannelException
import scala.concurrent.duration.*

object Server extends IOApp:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  given Slf4jFactory[IO] = Slf4jFactory.create[IO]

  override protected def reportFailure(err: Throwable): IO[Unit] =
    err match {
      case e: ClosedChannelException =>
        IO.unit
      case _ =>
        super.reportFailure(err)
    }

  def run(args: List[String]): IO[ExitCode] =
    ConfigUtils.readConfig(args.headOption.getOrElse("db_config.json"))
      .flatMap { config =>
        createDataSource(config, init = false).use { dataSource =>
          (for {
            _ <- Resource.eval(DBImpl.setupDataSource(dataSource))
            _ <- Resource.eval(Init.init(config))
          } yield ())
            .use(_ => IO.never)
            .as(ExitCode.Success)
        }
      }
