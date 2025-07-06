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
import cats.implicits.*
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryFollowMessagePlanner(
                                     userA: Int,
                                     userB: Int,
                                     override val planContext: PlanContext
                                   ) extends Planner[Boolean] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Boolean] = {
    for {
      // Step 1: Validate user IDs
      _ <- validateUserIDs()
      
      // Step 2: Check if userA follows userB
      isFollowing <- checkFollowStatus(userA, userB)
      _ <- IO(logger.info(s"[QueryFollow] UserA ${userA} follows UserB ${userB}: ${isFollowing}"))
    } yield isFollowing
  }

  private def validateUserIDs()(using PlanContext): IO[Unit] = {
    IO(logger.info("[validateUserIDs] Validating user IDs")) >>
    validateUserExists(userA, "userA") >>
    validateUserExists(userB, "userB")
  }

  private def validateUserExists(userID: Int, userType: String)(using PlanContext): IO[Unit] = {
    val sql = s"SELECT user_id FROM ${schemaName}.user_info_table WHERE user_id = ?;"
    readDBJsonOptional(sql, List(SqlParameter("Int", userID.toString))).map {
      case Some(_) => ()
      case None => throw InvalidInputException(s"User ${userType} (ID: ${userID}) does not exist")
    }
  }

  private def checkFollowStatus(userA: Int, userB: Int)(using PlanContext): IO[Boolean] = {
    val sql = s"SELECT * FROM ${schemaName}.follow_relation_table WHERE follower_id = ? AND followee_id = ?;"
    readDBJsonOptional(sql, List(
      SqlParameter("Int", userA.toString),
      SqlParameter("Int", userB.toString)
    )).map(_.isDefined)
  }
}
