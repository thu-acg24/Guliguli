package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.DanmakuService.Danmaku
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import io.circe.syntax.*
import org.joda.time.DateTime
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import io.circe.generic.auto._
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
import Objects.DanmakuService.Danmaku
import io.circe._
import io.circe.syntax._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryDanmakuByIDMessagePlanner(
  danmakuID: Int, 
  override val planContext: PlanContext
) extends Planner[Option[Danmaku]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[Danmaku]] = {
    for {
      // Step 1: Check if the danmakuID exists in DanmakuTable
      _ <- IO(logger.info(s"校验danmakuID=${danmakuID}是否存在于DanmakuTable中"))
      exists <- checkDanmakuIDExists(danmakuID)
      
      // Step 2: Return result based on existence
      result <- if (!exists) {
        IO(logger.info(s"danmakuID=${danmakuID}不存在，返回None")) >>
        IO.pure(None) // Explicitly return None if the ID doesn't exist
      } else {
        // Step 3: Fetch the Danmaku record
        IO(logger.info(s"danmakuID=${danmakuID}存在，开始查询对应记录")) >>
        fetchDanmakuByID(danmakuID)
      }
    } yield result
  }

  // Step 1.1: Check whether danmakuID exists
  private def checkDanmakuIDExists(danmakuID: Int)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         |SELECT EXISTS(
         |  SELECT 1
         |  FROM ${schemaName}.danmaku_table
         |  WHERE danmaku_id = ?
         |);
       """.stripMargin

    IO(logger.info(s"检查danmakuID存在性的SQL指令为: ${sql}")) >>
    readDBBoolean(sql, List(SqlParameter("Int", danmakuID.toString)))
  }

  // Step 2.1: Query for the actual danmaku record
  private def fetchDanmakuByID(danmakuID: Int)(using PlanContext): IO[Option[Danmaku]] = {
    val sql =
      s"""
         |SELECT danmaku_id, content, video_id, author_id, danmaku_color, time_in_video
         |FROM ${schemaName}.danmaku_table
         |WHERE danmaku_id = ?;
       """.stripMargin

    IO(logger.info(s"查询danmaku记录的SQL指令为: ${sql}")) >>
    readDBJsonOptional(sql, List(SqlParameter("Int", danmakuID.toString)))
      .map(_.map(decodeType[Danmaku])) // Decode the JSON result into Danmaku object
  }
}