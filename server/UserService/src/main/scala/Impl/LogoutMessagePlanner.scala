package Impl


import Common.API.PlanContext
import Common.APIException.InvalidInputException
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import cats.effect.IO
import cats.implicits._
import io.circe._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class LogoutMessagePlanner(
                                 token: String,
                                 override val planContext: PlanContext
                               ) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    val checkTokenSQL =
      s"SELECT token FROM $schemaName.token_table WHERE token = ?"
    val deleteTokenSQL =
      s"DELETE FROM $schemaName.token_table WHERE token = ?"
    val checkTokenParams = List(SqlParameter("String", token))
    val deleteTokenParams = List(SqlParameter("String", token))

    for {
      // Step 1: Check if the Token exists in TokenTable
      _ <- IO(logger.info(s"Checking if token '${token}' exists in the database."))
      tokenExists <- readDBBoolean(checkTokenSQL, checkTokenParams)
      _ <- if (!tokenExists) {
        IO(logger.warn(s"Token '${token}' does not exist in TokenTable.")) *>
          IO.raiseError(new InvalidInputException("登出失败，已登出"))
      } else IO.unit
      _ <- IO(logger.info(s"Token '${token}' exists. Proceeding with deletion..."))
      _ <- writeDB(deleteTokenSQL, deleteTokenParams).attempt.flatMap {
        case Right(_) =>
          IO(logger.info(s"Token '${token}' successfully removed from TokenTable."))
        case Left(e) =>
          IO(logger.error(s"Failed to delete Token '${token}' from TokenTable: ${e.getMessage}")) *>
            IO.raiseError(new InvalidInputException(s"删除Token失败：${e.getMessage}"))
      }
    } yield()
  }
}