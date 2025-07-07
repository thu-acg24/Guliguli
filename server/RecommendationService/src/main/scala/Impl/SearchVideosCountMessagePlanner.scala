package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import cats.effect.IO
import cats.implicits.*
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class SearchVideosCountMessagePlanner(
                                            searchString: String,
                                            override val planContext: PlanContext
                                          ) extends Planner[Option[Int]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[Int]] = {
    for {
      // Step 1: Log the start of the process
      _ <- IO(logger.info(s"开始查询符合标题条件的视频总数: 搜索关键词为 '${searchString}'"))

      // Step 2: Attempt to fetch the count of matching videos
      count <- getVideosCount
      
      // Step 3: Log the retrieved result
      _ <- IO(logger.info(s"查询结果: 符合条件的视频总数为 ${count.getOrElse(0)}"))
    } yield count
  }

  private def getVideosCount(using PlanContext): IO[Option[Int]] = {
    val sql =
      s"""
        SELECT COUNT(*)
        FROM ${schemaName}.video_info_table
        WHERE title ILIKE ?;
      """
    for {
      // Log the SQL query
      _ <- IO(logger.info(s"执行查询视频数量SQL指令: $sql"))
      
      // Execute the query and map the results
      queryResult <- readDBJsonOptional(
        sql,
        List(SqlParameter("String", s"%${searchString}%"))
      )
      videoCount <- IO {
        queryResult.map(json => decodeField[Int](json, "count"))
      }
    } yield videoCount
  }
}