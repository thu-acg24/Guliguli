package Process

import Common.API.{API, PlanContext, TraceID}
import Common.DBAPI.{initSchema, writeDB}
import Common.ServiceUtils
import Common.ServiceUtils.schemaName
import DBManager.Process.DBServer.setDBServer
import Global.DBConfig
import cats.effect.IO

import java.util.UUID

object Init {

  def init(config: DBConfig): IO[Unit] = {
    given PlanContext =
      PlanContext(traceID = TraceID(UUID.randomUUID().toString), 0)

    val program: IO[Unit] = for {
      _ <- API.init(config.maximumClientConnection)
      _ <- initSchema(schemaName)
      _ <- setDBServer()
    } yield ()
    program
  }

}
