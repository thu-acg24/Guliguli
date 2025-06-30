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
                                      ) extends Planner[Option[UserStat]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  /**
   * 实现计划（Plan）的主方法。
   * 
   * @return IO[Option[UserStat]] 包含用户统计数据的选项，如果找不到相关数据则返回None。
   */
  override def plan(using planContext: PlanContext): IO[Option[UserStat]] = {
    for {
      // Step 1: Validate userID and log the operation
      _ <- IO(logger.info(s"开始校验并查询用户ID: ${userID}"))
      userStatOpt <- fetchUserStat(userID) // Fetch user statistics
      _ <- IO(logger.info(s"查询结果: ${userStatOpt.getOrElse("未找到结果")}"))
    } yield userStatOpt
  }

  /**
   * 从数据库中获取用户统计信息。
   *
   * @param userID 用户ID
   * @return 一个包含UserStat的IO选项
   */
  private def fetchUserStat(userID: Int)(using PlanContext): IO[Option[UserStat]] = {
    if (userID <= 0) {
      // Step 1.1: Invalid userID
      IO(logger.warn(s"userID无效: ${userID}")) *> IO.pure(None)
    } else {
      // Step 2: Build the SQL query
      val sql =
        s"""
           |SELECT video_count, follower_count, following_count, favorite_video_count
           |FROM ${schemaName}.user_table
           |WHERE user_id = ?;
           """.stripMargin

      for {
        _ <- IO(logger.info(s"执行SQL查询语句: ${sql}"))
        queryParams = List(SqlParameter("Int", userID.toString)) // SQL参数
        result <- readDBJsonOptional(sql, queryParams) // 查询数据库
        userStatOpt <- result match {
          // Step 3: Decode the JSON result, if available
          case Some(json) =>
            val videoCount = decodeField[Int](json, "video_count")
            val followerCount = decodeField[Int](json, "follower_count")
            val followingCount = decodeField[Int](json, "following_count")
            val favoriteVideoCount = decodeField[Int](json, "favorite_video_count")
            IO(logger.info(s"成功解析JSON为UserStat对象")) >>
              IO.pure(Some(UserStat(followerCount, followingCount, videoCount, favoriteVideoCount)))
          case None =>
            IO(logger.warn(s"未找到userID对应的用户记录: ${userID}")) >> IO.pure(None)
        }
      } yield userStatOpt
    }
  }
}