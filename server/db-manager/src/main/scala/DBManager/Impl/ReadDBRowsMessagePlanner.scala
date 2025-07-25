package DBManager.Impl

import Common.API.PlanContext
import Common.DBAPI.snakeToCamel
import Common.Object.SqlParameter
import DBImpl.prepareParameter
import Global.DBConfig
import cats.effect.{IO, Ref}
import io.circe.Json
import org.postgresql.jdbc.PgArray
import io.circe.{Json, parser}

import java.sql.{Connection, Timestamp}
import scala.collection.mutable.ListBuffer

case class ReadDBRowsMessagePlanner(sqlQuery: String,
                                    parameters: List[SqlParameter],
                                    override val planContext: PlanContext,

                                   ) extends DBPlanner[List[Json]] {
  override def planWithConnection(connection: Connection, connectionMap: Ref[IO, Map[String, Connection]]): IO[List[Json]] = IO.delay {
    val preparedStatement = connection.prepareStatement(sqlQuery)
    try {
      println(preparedStatement)
      // Populate the prepared statement with parameters
      parameters.zipWithIndex.foreach { case (param, index) =>
        prepareParameter(preparedStatement, param, index+1)
      }

      val resultSet = preparedStatement.executeQuery()
      val metaData = resultSet.getMetaData
      val columns = metaData.getColumnCount
      val buffer = ListBuffer.empty[Json]

      while (resultSet.next()) {
        val rowJson = (1 to columns).foldLeft(Json.obj()) { (jsonObj, columnIndex) =>
          val columnName = metaData.getColumnName(columnIndex)
          val columnType = metaData.getColumnTypeName(columnIndex).toLowerCase
          val value = resultSet.getObject(columnIndex)

          println(columnName)
          println(columnType)
          println(value)

          val processedValue: Json = value match {
            case null if columnType.startsWith("_") => Json.arr() // Handle null for array types as empty array
            case array: PgArray => Json.arr(array.getArray.asInstanceOf[Array[?]].map {
              case s: String => Json.fromString(s)
              case i: Int => Json.fromInt(i)
              case b: Boolean => Json.fromBoolean(b)
              case l: Long => Json.fromLong(l)
              case d: Double => Json.fromDoubleOrNull(d)
              case _ => Json.Null
            }*)
            case v: Timestamp => Json.fromString(v.getTime.toString) // Convert Timestamp to String
            case s: String if columnType == "vector" =>
              parser.parse(s).getOrElse {
                val cleaned = s.stripPrefix("[").stripSuffix("]")
                val floatArray = cleaned.split(",").map(_.trim).filter(_.nonEmpty).flatMap { num =>
                  scala.util.Try(num.toFloat).toOption
                }
                Json.arr(floatArray.map(Json.fromFloatOrNull)*)
              }
            case s: String if columnName.endsWith("_") => parser.parse(s).fold(throw _, a=>a)
            case s: String => Json.fromString(s)
            case i: Integer => Json.fromInt(i)
            case b: java.lang.Boolean => Json.fromBoolean(b)
            case l: java.lang.Long => Json.fromLong(l)
            case bd: java.math.BigDecimal => Json.fromBigDecimal(bd)
            case null => Json.Null
            case _ => Json.fromString(value.toString) // Fallback for unsupported types
          }

          println(processedValue)

          jsonObj.mapObject(_.add(snakeToCamel(columnName), processedValue))
        }
        buffer += rowJson
      }
      println("2")
      println(buffer.toList)

      buffer.toList
    } finally {
      preparedStatement.close() // Ensure the PreparedStatement is closed after use
    }
  }
}