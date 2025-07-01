package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.DanmakuService.Danmaku
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax.*
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryDanmakuByIDMessagePlanner(
  danmakuID: Int, 
  override val planContext: PlanContext
) extends Planner[Danmaku] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Danmaku] = {
    for {
      result <- fetchDanmakuByID(danmakuID)
    } yield result
  }

  // Step 2.1: Query for the actual danmaku record
  private def fetchDanmakuByID(danmakuID: Int)(using PlanContext): IO[Danmaku] = {
    val sql =
      s"""
         |SELECT danmaku_id, content, video_id, author_id, danmaku_color, time_in_video
         |FROM ${schemaName}.danmaku_table
         |WHERE danmaku_id = ?;
       """.stripMargin

    IO(logger.info(s"查询danmaku记录的SQL指令为: ${sql}")) >>
    readDBJsonOptional(sql, List(SqlParameter("Int", danmakuID.toString)))
      .flatMap {
        case Some(json) =>
          json.as[Danmaku] match {
            case Right(danmaku) =>
              IO(logger.info(s"成功查询到danmaku记录: ${danmaku}")) >> IO(danmaku)
            case Left(error) =>
              IO(logger.error(s"解析danmaku记录失败: ${error.getMessage}")) >>
              IO.raiseError(IllegalArgumentException("Failed to parse danmaku record"))
          }
        case None =>
          IO(logger.error(s"未找到danmaku记录，ID: ${danmakuID}")) >>
          IO.raiseError(IllegalArgumentException(s"No danmaku found with ID: $danmakuID"))
      }
  }
}