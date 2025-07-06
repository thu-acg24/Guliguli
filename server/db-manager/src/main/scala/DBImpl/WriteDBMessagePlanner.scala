package DBImpl

import Common.API.{PlanContext, TraceID}
import Common.Object.{ParameterList, SqlParameter}

import cats.effect.{IO, Ref}
import org.joda.time.format.DateTimeFormat

import java.sql
import java.sql.{Connection, PreparedStatement, Timestamp}

case class WriteDBMessagePlanner(sqlStatement: String, parameters: List[SqlParameter]) extends DBPlanner[String] {
  override def planWithConnection(connection: Connection)(using planContext:PlanContext): IO[String] = IO.delay{
    val preparedStatement = connection.prepareStatement(sqlStatement)
    try {
      if (parameters.isEmpty)
        preparedStatement.executeUpdate()
      else {
        // Reset the statement for each set of parameters
        preparedStatement.clearParameters()

        // Set parameters for the current execution
        parameters.zipWithIndex.foreach { case (sqlParameter, index) =>
          prepareParameter(preparedStatement, sqlParameter, index+1)
        }

        // Execute the update for the current set of parameters
        preparedStatement.executeUpdate()
      }
      "Operation(s) done successfully"
    } finally {
      preparedStatement.close() // Ensure the PreparedStatement is closed after use
    }
  }
}