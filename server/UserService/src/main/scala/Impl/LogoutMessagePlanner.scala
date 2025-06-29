package Impl


import Utils.AuthProcess.{validateToken, invalidateToken}
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import cats.effect.IO
import cats.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName
import Utils.AuthProcess.validateToken
import Utils.AuthProcess.invalidateToken
import Utils.AuthProcess.invalidateToken
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class LogoutMessagePlanner(
                                 token: String,
                                 override val planContext: PlanContext
                               ) extends Planner[Option[String]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1. Validate the token
      _ <- IO(logger.info(s"[Step 1] Validating token: '${token}'"))
      validationResult <- validateToken(token)

      // Step 2. Process token validation result
      result <- validationResult match {
        case None =>
          // If token is invalid, return specific error message
          IO(logger.info(s"[Step 1.1] Token '${token}' is invalid. Returning an error."))
            .as(Some("Invalid Token"))

        case Some(_) =>
          // If token is valid, proceed to invalidate it
          invalidateToken(token)
      }
    } yield result
  }
}