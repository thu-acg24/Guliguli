package Impl


import Common.API.PlanContext
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
    for {
      _ <- IO(logger.info(s"开始查询 userID=${userID} 的粉丝列表"))
      // Step 1: 从 FollowRelationTable 获取相关记录
      followRelations <- queryFollowRelations(userID)

      _ <- IO(logger.info(s"查询到 ${followRelations.size} 条记录"))
      result <- if (followRelations.isEmpty) {
        // Step 1.1: 查询结果为空时，返回空列表
        IO(logger.info(s"userID=${userID} 无关注记录，返回空列表")) >>
          IO.pure(List.empty[FollowRelation])
      } else {
        for {
          // Step 2: 按 timestamp 降序排序
          _ <- IO(logger.info("对查询的记录按时间戳降序排序"))
          sortedRelations <- IO(followRelations.sortBy(_.timestamp.getMillis)(Ordering[Long].reverse))

          // Step 3: 截取第 rangeL 到 rangeR 条记录（索引从 rangeL-1 到 rangeR-1）
          _ <- IO(logger.info(s"截取记录范围，范围为 rangeL=${rangeL}, rangeR=${rangeR}"))
          slicedRelations <- IO(sliceFollowRelations(sortedRelations, rangeL, rangeR))
        } yield slicedRelations
      }
    } yield result
  }

  private def queryFollowRelations(userID: Int)(using PlanContext): IO[List[FollowRelation]] = {
    val sql =
      s"""
         |SELECT follower_id, followee_id, timestamp
         |FROM ${schemaName}.follow_relation_table
         |WHERE followee_id = ?;
         """.stripMargin
    IO(logger.info(s"构建 SQL 查询语句: $sql")) >>
      readDBRows(sql, List(SqlParameter("Int", userID.toString))).map { rows =>
        rows.map(decodeType[FollowRelation])
      }
  }

  private def sliceFollowRelations(
      sortedRelations: List[FollowRelation],
      rangeL: Int,
      rangeR: Int
  ): List[FollowRelation] = {
    if (rangeL > rangeR || rangeL < 1 || rangeR > sortedRelations.size) {
      logger.info(
        s"输入范围无效或超出结果范围，返回空列表。实际范围：totalSize=${sortedRelations.size}, rangeL=${rangeL}, rangeR=${rangeR}")
      List.empty
    } else {
      sortedRelations.slice(rangeL - 1, rangeR)
    }
  }
}