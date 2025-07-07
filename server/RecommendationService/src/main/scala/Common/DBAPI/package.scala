package Common

package object DBAPI {


import Common.API.PlanContext
import Common.API.TraceID
import Common.Object.ParameterList
import Common.Object.SqlParameter
import DBAPI.EndTransactionMessage
import DBAPI.InitSchemaMessage
import DBAPI.ReadDBRowsMessage
import DBAPI.ReadDBValueMessage
import DBAPI.StartTransactionMessage
import DBAPI.WriteDBListMessage
import DBAPI.WriteDBMessage
import Global.DBConfig
import cats.effect.*
import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.parser.decode
import org.http4s.client.Client
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

  def startTransaction[A](block: PlanContext ?=> IO[A])(using encoder: Encoder[A], ctx: PlanContext): IO[A] = {
    given newContext: PlanContext = ctx.copy(transactionLevel = ctx.transactionLevel + 1)

    // Define the start transaction action
    val startTransactionAction = if (ctx.transactionLevel == 0) {
      StartTransactionMessage().send
    } else {
      IO.unit // No action needed, already inside a transaction
    }

    def commitOrRollbackAction(result: Either[Throwable, A]): IO[A] =
      result match {
        case Left(exception:DidRollbackException) =>
          IO.raiseError(exception)   /** 如果问题已经处理过了，我们不需要额外处理了 */
        case Left(exception)=>
          EndTransactionMessage(false).send >> IO.raiseError(DidRollbackException(exception.getMessage)) // 出现了问题，回滚
        case Right(value) =>
          if (ctx.transactionLevel == 0)
            /** 除非是第一层，否则是不把事务结束的 */
            EndTransactionMessage(true).send.as(value)
          else IO.pure(value)
      }

    for {
      _ <- startTransactionAction // Start the transaction if this is the first level
      result <- block(using newContext).attempt // Execute the block with the new (incremented) transaction context
      _ <- IO.println("Step result")

      _ <- result match
        case Left(value) => IO.pure(value.printStackTrace())
        case Right(value) => IO.println(s"result = $result")

      finalResult <- commitOrRollbackAction(result)
    } yield finalResult
  }

  def rollback(): IO[Unit] = IO.raiseError(RollbackException("Rollback"))

  def initSchema(schemaName: String)(using planContext:PlanContext): IO[String] = InitSchemaMessage(schemaName).send

  def readDBRows(sqlQuery: String, parameters: List[SqlParameter])(using PlanContext): IO[List[Json]] =
    ReadDBRowsMessage(sqlQuery, parameters).send
    
  def readDBJson(sqlQuery: String, parameters: List[SqlParameter])(using PlanContext): IO[Json] =
    ReadDBRowsMessage(sqlQuery, parameters).send.map(_.head)

  def readDBJsonOptional(sqlQuery: String, parameters: List[SqlParameter])(using PlanContext): IO[Option[Json]] =
    ReadDBRowsMessage(sqlQuery, parameters).send.map(_.headOption)

  def readDBInt(sqlQuery: String, parameters: List[SqlParameter])(using context: PlanContext): IO[Int] =
    for {
      resultParam: String<- ReadDBValueMessage(sqlQuery, parameters).send
      convertedResult = resultParam.toInt
    } yield convertedResult

  def readDBString(sqlQuery: String, parameters: List[SqlParameter])(using context: PlanContext): IO[String] =
    for {
      resultParam: String<- ReadDBValueMessage(sqlQuery, parameters).send
    } yield resultParam

  def readDBBoolean(sqlQuery: String, parameters: List[SqlParameter])(using context: PlanContext): IO[Boolean] =
    for {
      resultParam: String<- ReadDBValueMessage(sqlQuery, parameters).send
      convertedResult = resultParam.startsWith("t")
    } yield convertedResult

  def writeDB(sqlQuery: String, parameters: List[SqlParameter])(using PlanContext): IO[String] = WriteDBMessage(sqlQuery, parameters).send

  def writeDBList(sqlQuery: String, parameters: List[ParameterList])(using PlanContext): IO[String] = WriteDBListMessage(sqlQuery, parameters).send

  def decodeField[T: Decoder](json:Json, field:String):T={
    json.hcursor.downField(snakeToCamel(field)).as[T].fold(throw _, value=>value)
  }
  def decodeType[T:Decoder](json:Json):T={
    json.as[T].fold(throw _, value=>value)
  }
  def decodeTypeIO[T:Decoder](json:Json):IO[T]={
    json.as[T].fold(IO.raiseError, IO.pure)
  }
  def decodeType[T:Decoder](st:String):T={
    decode[T](st).fold(throw _, value=>value)
  }
  def decodeTypeIO[T:Decoder](st:String):IO[T]={
    decode[T](st).fold(IO.raiseError, IO.pure)
  }

  def snakeToCamel(snake: String): String = {
    snake.split("_").toList match {
      case head :: tail =>
        head + tail.map {
          case "id" => "ID"
          case other => other.capitalize
        }.mkString
      case Nil => ""
    }
  }
}