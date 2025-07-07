package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserStat
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * Planner for QueryUserStatMessage: 根据用户ID查询用户表，返回用户的统计数据。
 *
 * @param userID 用户ID
 * @param planContext 上下文环境
 */

case class QueryUserStatMessagePlanner(
                                        userID: Int,
                                        override val planContext: PlanContext
                                      ) extends Planner[UserStat] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[UserStat] = {
    val sqlFollowers =
      s"""
         |SELECT COUNT(*) AS count
         |FROM $schemaName.follow_relation_table
         |WHERE followee_id = ?;
           """.stripMargin
    val sqlFollowings =
      s"""
         |SELECT COUNT(*) AS count
         |FROM $schemaName.follow_relation_table
         |WHERE follower_id = ?;
           """.stripMargin
    val queryParams = List(SqlParameter("Int", userID.toString)) // SQL参数
    for {
      // Step 1: Validate userID and log the operation
      _ <- IO(logger.info(s"开始查询数据库: $userID"))
      resultFollower <- readDBInt(sqlFollowers, queryParams)
      resultFollowing <- readDBInt(sqlFollowings, queryParams)
    } yield UserStat(resultFollower, resultFollowing)
  }
}