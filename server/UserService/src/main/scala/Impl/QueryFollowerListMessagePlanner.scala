package Impl


import Common.API.PlanContext
import Common.APIException.InvalidInputException
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.FollowRelation
import Objects.UserService.User
import Objects.UserService.UserRole
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryFollowerListMessagePlanner(
    userID: Int,
    rangeL: Int,
    rangeR: Int,
    override val planContext: PlanContext
) extends Planner[List[FollowRelation]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[FollowRelation]] = {
    val sql =
      s"""
         |SELECT follower_id, followee_id, timestamp
         |FROM ${schemaName}.follow_relation_table
         |WHERE followee_id = ?
         |ORDER BY timestamp DESC
         |OFFSET ? LIMIT ?;
         |""".stripMargin
    val param = List(
      SqlParameter("Int", userID.toString),
      SqlParameter("Int", (rangeL - 1).toString),
      SqlParameter("Int", (rangeR - rangeL).toString)
    )
    for {
      _ <- IO.unit.ensure(InvalidInputException("Invalid range")) { _ =>
        rangeL > 0 && rangeR <= 501 && rangeL <= rangeR
      }
      // Step 1: Query following based on userID
      _ <- IO(logger.info(s"从关注关系表中查询UserID=${userID}的粉丝"))
      followerRecords <- readDBRows(sql, param)

      // Step 2: If followeeRecords is empty, return an empty list
      _ <- IO(logger.info(s"检查查询结果是否为空"))
      result <- if (followerRecords.isEmpty) {
        IO(logger.info("查询结果为空，返回空列表")) >> IO.pure(List.empty[FollowRelation])
      } else {
        // Step 3: Process follower records
        processFollowerRecords(followerRecords)
      }
    } yield result
  }

  /** Step 3: Process follower records */
  private def processFollowerRecords(followerRecords: List[Json])(using PlanContext): IO[List[FollowRelation]] = {
    for {
      followList <- IO {
        followerRecords.map(record => decodeType[FollowRelation](record))
      }
    } yield followList
  }
}