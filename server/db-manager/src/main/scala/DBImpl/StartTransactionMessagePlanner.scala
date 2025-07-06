package DBImpl

import Common.API.{PlanContext, TraceID}
import cats.effect.{IO, Ref}

import java.sql.Connection

// StartTransactionMessage case class
case class StartTransactionMessagePlanner() extends DBPlanner[String] {
  override def planWithConnection(connection: Connection)(using planContext:PlanContext): IO[String] = IO.delay{
    // Set auto-commit to false to start a new transaction
    connection.setAutoCommit(false)
    "Transaction started"
  }
}