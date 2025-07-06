package DBManager.Impl

import Common.API.{PlanContext, TraceID}
import Common.Object.{ParameterList, SqlParameter}
import Global.DBConfig
import DBImpl.prepareParameter
import cats.effect.{IO, Ref}
import org.joda.time.format.DateTimeFormat

import java.sql
import java.sql.{Connection, PreparedStatement, Timestamp}

case class WriteDBListMessagePlanner(sqlStatement: String, 
                                     parameters: List[ParameterList], 
                                     override val planContext: PlanContext,

                                    ) extends DBPlanner[String] {
  override def planWithConnection(connection: Connection, connectionMap: Ref[IO, Map[String, Connection]]): IO[String] = IO.delay{
    val preparedStatement = connection.prepareStatement(sqlStatement)
    try {
      if (parameters.isEmpty)
        preparedStatement.executeUpdate()
      else
        parameters.foreach { parameterList =>
          // Reset the statement for each set of parameters
          preparedStatement.clearParameters()

          // Set parameters for the current execution
          parameterList.l.zipWithIndex.foreach { case (sqlParameter, index) =>
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