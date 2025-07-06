package Common

import Common.API.{PlanContext, TraceID}
import Global.DBConfig
import Common.Object.{ParameterList, SqlParameter}
import DBImpl.{EndTransactionMessagePlanner, InitSchemaMessagePlanner, ReadDBRowsMessagePlanner, ReadDBValueMessagePlanner, StartTransactionMessagePlanner, WriteDBListMessagePlanner, WriteDBMessagePlanner, callDBAPI}
import cats.effect.*
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.auto.*
import org.joda.time.DateTime
import io.circe.parser.decode
import org.http4s.client.Client
import org.joda.time.format.ISODateTimeFormat


package object DBAPI {
  given Conversion[DateTime, String] = dateTime => {
    dateTime.getMillis.toString
  }
  given Conversion[String, DateTime] = time=> {
    new DateTime(time.toLong)
  }

  def startTransaction[A](block: PlanContext ?=> IO[A])(using encoder: Encoder[A], ctx: PlanContext): IO[A] = {
    given newContext: PlanContext = ctx.copy(transactionLevel = ctx.transactionLevel + 1)

    // Define the start transaction action
    val startTransactionAction = if (ctx.transactionLevel == 0) {
      StartTransactionMessagePlanner().send
    } else {
      IO.unit // No action needed, already inside a transaction
    }

    def commitOrRollbackAction(result: Either[Throwable, A]): IO[A] =
      result match {
        case Left(exception:DidRollbackException) =>
          exception.printStackTrace()
          IO.raiseError(exception)   /** 如果问题已经处理过了，我们不需要额外处理了 */
        case Left(exception)=>
          exception.printStackTrace()
          EndTransactionMessagePlanner(false).send *> IO.raiseError(DidRollbackException(exception.getMessage)) // 出现了问题，回滚
        case Right(value) =>
          if (ctx.transactionLevel == 0)
            /** 除非是第一层，否则是不把事务结束的 */
            EndTransactionMessagePlanner(true).send.as(value)
          else IO.pure(value)
      }

    for {
      _ <- startTransactionAction // Start the transaction if this is the first level
      result <- block(using newContext).attempt // Execute the block with the new (incremented) transaction context

      _ <- result match
        case Left(err) => IO { println(s"${ctx.traceID.id}, ${ctx.transactionLevel} finish, error = "); err.printStackTrace() }
        case Right(value) => IO.println(s"${ctx.traceID.id}, ${ctx.transactionLevel} finish, result = ${value}")

      finalResult <- commitOrRollbackAction(result)
    } yield finalResult
  }

  def rollback(): IO[Unit] = IO.raiseError(RollbackException("Rollback"))

  def initSchema(schemaName: String)(using planContext:PlanContext): IO[String] = InitSchemaMessagePlanner(schemaName).send

  def readDBRows(sqlQuery: String, parameters: List[SqlParameter])(using PlanContext): IO[List[Json]] =
    ReadDBRowsMessagePlanner(sqlQuery, parameters).send


  def readDBJson(sqlQuery: String, parameters: List[SqlParameter])(using PlanContext): IO[Json] =
    ReadDBRowsMessagePlanner(sqlQuery, parameters).send.map(_.head)
    
  def readDBJsonOptional(sqlQuery: String, parameters: List[SqlParameter])(using PlanContext): IO[Option[Json]] =
    ReadDBRowsMessagePlanner(sqlQuery, parameters).send.map(_.headOption)

  def readDBInt(sqlQuery: String, parameters: List[SqlParameter])(using context: PlanContext): IO[Int] =
    for {
      resultParam: String<- ReadDBValueMessagePlanner(sqlQuery, parameters).send
      convertedResult = resultParam.toInt
    } yield convertedResult

  def readDBString(sqlQuery: String, parameters: List[SqlParameter])(using context: PlanContext): IO[String] =
    for {
      resultParam: String<- ReadDBValueMessagePlanner(sqlQuery, parameters).send
    } yield resultParam

  def readDBBoolean(sqlQuery: String, parameters: List[SqlParameter])(using context: PlanContext): IO[Boolean] =
    for {
      resultParam: String<- ReadDBValueMessagePlanner(sqlQuery, parameters).send
      convertedResult = resultParam.startsWith("t")
    } yield convertedResult

  def writeDB(sqlQuery: String, parameters: List[SqlParameter])(using PlanContext): IO[String] = WriteDBMessagePlanner(sqlQuery, parameters).send

  def writeDBList(sqlQuery: String, parameters: List[ParameterList])(using PlanContext): IO[String] = WriteDBListMessagePlanner(sqlQuery, parameters).send

  implicit val dateTimeDecoder: Decoder[DateTime] = new Decoder[DateTime] {
    private val formatter = ISODateTimeFormat.dateTimeParser()

    override def apply(c: HCursor): Decoder.Result[DateTime] = {
      c.as[String].map(formatter.parseDateTime)
    }
  }

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
