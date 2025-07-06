package DBImpl

import Common.API.{PlanContext, TraceID}
import Common.Object.SqlParameter
import DBImpl.DBPlanner

import cats.effect.{IO, Ref}
import org.joda.time.format.DateTimeFormat

import java.sql.{Connection, Timestamp}

case class ReadDBValueMessagePlanner(sqlQuery: String, parameters: List[SqlParameter]) extends DBPlanner[String] {
  override def planWithConnection(connection: Connection)(using planContext:PlanContext): IO[String] = IO.delay{
    val preparedStatement = connection.prepareStatement(sqlQuery)
    try {
      // Populate the prepared statement with parameters
      parameters.zipWithIndex.foreach { case (param, index) =>
        prepareParameter(preparedStatement, param, index + 1)
      }
      // Execute the query
      val resultSet = preparedStatement.executeQuery()
      if (resultSet.next()) {
        resultSet.getString(1) // Assuming the value you want is in the first column
      } else {
        throw new NoSuchElementException("No value found for the given query and parameters.")
      }
    } finally {
      preparedStatement.close() // Ensure the PreparedStatement is closed after use
    }
  }
}