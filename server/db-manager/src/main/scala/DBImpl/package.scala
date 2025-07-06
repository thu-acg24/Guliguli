import Common.API.PlanContext
import cats.effect.*
import com.zaxxer.hikari.HikariDataSource
import io.circe.*
import io.circe.generic.auto.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import java.sql.{Connection, SQLException}
import scala.concurrent.duration.*
import Common.Object.SqlParameter
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.parser.parse
import org.postgresql.util.PSQLException

import java.sql.{PreparedStatement, Timestamp, Types}
import scala.compiletime.uninitialized
import io.circe.parser.decode
import io.circe.syntax._ // for .asJson


package object DBImpl {
  private val dbDateFmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
  private var dataSource: HikariDataSource = uninitialized
  var connectionMap: Ref[IO, Map[String, Connection]] = uninitialized

  def setupDataSource(ds: HikariDataSource): IO[Unit] = {
    IO {
      dataSource = ds
    } >>
      Ref.of[IO, Map[String, Connection]](Map.empty).flatMap(r => IO {
        connectionMap = r
      })
  }

  def callDBAPI[T](planner: DBPlanner[T])(using planContext: PlanContext): IO[T] = {
    // 日志输出: 确保打印出 API 名称，planContext 上下文引用，且确保 planner 的完整信息一行展示, 且没有过多的空格、换行符干扰阅读
    IO.println(s"${DateTime.now().toString(dbDateFmt)} INFO DBOperation: ${planContext}, info = ${planner.toString.replaceAll(" {4,}", "   ").replaceAll("[\\n\\r]+", " ")}") >> {
      /** 只有是在transaction的情况下，我们才需要保存connection，否则是不需要的！ */
      if (planContext.transactionLevel > 0)
        connectionMap.get.flatMap { map =>
          map.get(planContext.traceID.id) match {
            case Some(existingConnection) =>
              // Use the existing connection from the map
              planner.planWithConnection(existingConnection).handleErrorWith {
                case error: SQLException if error.getMessage.contains("Connection is closed") =>
                  connectionMap.update(_ - planContext.traceID.id) >> callDBAPI(planner) // update connection map and redo the operation.
                case other => IO.raiseError(other) // Rethrow any other exception
              }
            case None =>
              for {
                newConnection <- IO(dataSource.getConnection)
                _ <- connectionMap.update(_ + (planContext.traceID.id -> newConnection))
                result <- planner.planWithConnection(newConnection)
                _ <- (for {
                  _ <- IO.sleep(5.minutes)
                  _ <- connectionMap.update(_ - planContext.traceID.id)
                  _ <- IO(newConnection.close())
                } yield ()).start // Run the cleanup process asynchronously
              } yield result
          }
        }
      else
        Resource.make(IO(dataSource.getConnection)) { connection => IO(connection.close()) }.use {
          newConnection =>
            planner.planWithConnection(newConnection)
        }
    }
  }


  def prepareParameter(statement: PreparedStatement, sqlParameter: SqlParameter, index: Int): Unit = {
    sqlParameter.dataType.toLowerCase match {
//      case "string" if sqlParameter.value == null => statement.setNull(index, Types.VARCHAR)
//      case "string" => statement.setString(index, decode[String](sqlParameter.value).getOrElse(""))

      case "string" if sqlParameter.value == null => statement.setNull(index, Types.VARCHAR)
      case "string" => statement.setString(index, sqlParameter.value)

      case "int" if sqlParameter.value == null => statement.setNull(index, Types.INTEGER)
      case "int" => statement.setInt(index, sqlParameter.value.toInt)

      case "bigint" if sqlParameter.value == null => statement.setNull(index, Types.BIGINT)
      case "bigint" => statement.setLong(index, sqlParameter.value.toLong)

      case "long" if sqlParameter.value == null => statement.setNull(index, Types.BIGINT)
      case "long" => statement.setLong(index, sqlParameter.value.toLong)

      case "boolean" if sqlParameter.value == null => statement.setNull(index, Types.BOOLEAN)
      case "boolean" => statement.setBoolean(index, sqlParameter.value.toBoolean)

      case "double" if sqlParameter.value == null => statement.setDouble(index, Types.DOUBLE)
      case "double" => statement.setDouble(index, sqlParameter.value.toDouble)
      
      case "float" if sqlParameter.value == null => statement.setNull(index, Types.FLOAT)
      case "float" => statement.setFloat(index, sqlParameter.value.toFloat)

      case "datetime" if sqlParameter.value == null => statement.setNull(index, Types.TIMESTAMP)
      case "datetime" => statement.setTimestamp(index, new Timestamp(sqlParameter.value.toLong)) // Convert DateTime to Timestamp          // Add more type cases as needed

      case "array[int]" =>
        if (sqlParameter.value == null || sqlParameter.value.isEmpty || sqlParameter.value=="[]") {
          // 创建一个空的 SQL INTEGER 数组
          val array = statement.getConnection.createArrayOf("INTEGER", Array.empty[AnyRef])
          statement.setArray(index, array)
        } else {
          // 解析字符串为 Array[Int]
          val intArray: Array[Int] = sqlParameter.value
            .replace("[", "").replace("]", "") // 去掉字符串中的方括号
            .split(",") // 分割字符串为数组
            .map(_.trim.toInt) // 转换为整数数组
          val array = statement.getConnection.createArrayOf("INTEGER", intArray.map(_.asInstanceOf[AnyRef]))
          statement.setArray(index, array)
        }

      case "array[bigint]" =>
        if (sqlParameter.value == null || sqlParameter.value.isEmpty || sqlParameter.value=="[]") {
          // Create an empty SQL BIGINT array
          val array = statement.getConnection.createArrayOf("BIGINT", Array.empty[AnyRef])
          statement.setArray(index, array)
        } else {
          // Parse the string into an Array[Long]
          val longArray: Array[Long] = sqlParameter.value
            .replace("[", "").replace("]", "") // Remove square brackets
            .split(",") // Split into elements
            .filter(_.nonEmpty)
            .map(_.trim.toLong) // Convert to Long
          // Convert to Array[AnyRef] for JDBC
          val array = statement.getConnection.createArrayOf("BIGINT", longArray.map(_.asInstanceOf[AnyRef]))
          statement.setArray(index, array)
        }

//      case "array[string]" =>
//        val parsedJson: Json = parse(sqlParameter.value) match {
//          case Right(json) => json
//          case Left(error) => throw new Exception(s"Error parsing JSON: ${error.getMessage}")
//        }
//        val jsonArray = parsedJson.asArray.getOrElse(throw new PSQLException("Expected a JSON array", null))
//        val stringList: List[String] = jsonArray.map {
//          _.noSpaces
//        }.toList // You can change how to extract data from each JSON element if needed
//        val array: Array[AnyRef] = stringList.map(decode[String](_).toOption.get).map(_.asInstanceOf[AnyRef]).toArray
//        val sqlArray = statement.getConnection.createArrayOf("VARCHAR", array)
//        statement.setArray(index, sqlArray)

      case "array[string]" =>
        parse(sqlParameter.value) match {
          case Right(json) =>
            val jsonArray = json.asArray.getOrElse(throw new PSQLException("Expected a JSON array", null))
            val stringList: List[String] = jsonArray.map {
              _.noSpaces
            }.toList // You can change how to extract data from each JSON element if needed
            val array: Array[AnyRef] = stringList.map(_.asInstanceOf[AnyRef]).toArray
            // Create the SQL Array using the connection and set it in the prepared statement
            val sqlArray = statement.getConnection.createArrayOf("VARCHAR", array)
            statement.setArray(index, sqlArray)

          case Left(error) =>
            if (sqlParameter.value.startsWith("[") && sqlParameter.value.endsWith("]")){
              val array: Array[AnyRef] = sqlParameter.value.substring(1, sqlParameter.value.length - 1).split(",").map(_.asInstanceOf[AnyRef])
              val sqlArray = statement.getConnection.createArrayOf("VARCHAR", array)
              statement.setArray(index, sqlArray)
            }
            else
              throw new Exception(s"Error parsing JSON: ${error.getMessage}")
        }

      case "vector" =>
        if (sqlParameter.value == null || sqlParameter.value.isEmpty)
          throw new IllegalArgumentException("Vector value cannot be null or empty")

        parse(sqlParameter.value) match {
          case Right(json) =>
            val vectorValues: Array[Float] = json.asArray.getOrElse(
              throw new PSQLException("Expected a JSON array", null)
            ).map { item =>
              item.asNumber.map(x => x.toDouble).getOrElse(
                throw new PSQLException("Expected numeric values in the vector", null)
              ).toFloat
            }.toArray

            val vectorString = vectorValues.mkString(",")
            statement.setObject(index, s"[$vectorString]", Types.OTHER)

          case Left(error) =>
            throw new IllegalArgumentException(s"Error parsing vector JSON: ${error.getMessage}")
        }

      case _ => throw new IllegalArgumentException(s"Unhandled parameter type: ${sqlParameter.dataType}")
    }
  }


}
