package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.FollowRelation
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryFollowingListMessagePlanner(
                                             userID: Int,
                                             rangeL: Int,
                                             rangeR: Int,
                                             override val planContext: PlanContext
                                           ) extends Planner[List[FollowRelation]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[FollowRelation]] = {
    for {
      // Step 1: Query followees based on userID
      _ <- IO(logger.info(s"从关注关系表中查询UserID=${userID}的关注者"))
      followeeRecords <- queryFolloweeRecords()

      // Step 2: If followeeRecords is empty, return an empty list
      _ <- IO(logger.info(s"检查查询结果是否为空"))
      result <- if (followeeRecords.isEmpty) {
        IO(logger.info("查询结果为空，返回空列表")) *> IO.pure(List.empty[FollowRelation])
      } else {
        // Step 3-5: Process followee records (Sort, Paginate, Transform)
        processFolloweeRecords(followeeRecords)
      }
    } yield result
  }

  /** Step 1: Query Followee Records */
  private def queryFolloweeRecords()(using PlanContext): IO[List[Json]] = {
    val sql =
      s"""
         |SELECT follower_id, followee_id, timestamp
         |FROM ${schemaName}.follow_relation_table
         |WHERE follower_id = ?;
         |""".stripMargin

    IO(logger.info(s"执行查询指令：${sql}，使用参数: UserID=${userID}")) >>
      readDBRows(sql, List(SqlParameter("Int", userID.toString)))
  }

  /** Step 3-5: Process followee records (Sort, Paginate, Transform) */
  private def processFolloweeRecords(followeeRecords: List[Json])(using PlanContext): IO[List[FollowRelation]] = {
    for {
      // Step 3: Sort records by timestamp in ascending order
      _ <- IO(logger.info(s"对查询结果按照timestamp字段升序排序"))
      sortedRecords <- IO {
        followeeRecords.sortBy(record => decodeField[DateTime](record, "timestamp"))
      }

      // Step 4: Apply pagination based on rangeL and rangeR
      _ <- IO(logger.info(s"根据分页范围rangeL=${rangeL}和rangeR=${rangeR}进行分页提取"))
      paginatedRecords <- IO {
        sortedRecords.slice(rangeL, rangeR)
      }

      // Step 5: Transform paginated records into FollowRelation objects
      _ <- IO(logger.info(s"将分页后的记录封装为FollowRelation格式的对象"))
      followList <- IO {
        paginatedRecords.map(record => decodeType[FollowRelation](record))
      }
    } yield followList
  }
}