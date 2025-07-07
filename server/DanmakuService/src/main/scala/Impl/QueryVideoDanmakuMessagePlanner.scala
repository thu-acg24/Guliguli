package Impl


import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.DanmakuService.Danmaku
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryVideoDanmakuMessagePlanner(videoID: Int, token: Option[String], override val planContext: PlanContext) extends Planner[List[Danmaku]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[Danmaku]] = {
    for {
      result <- fetchDanmakuRecords()
      _ <- IO(logger.info(s"查询到 ${result.size} 条弹幕记录"))
    } yield result
  }

  /**
   * 查询与视频关联的弹幕记录
   * @return List[Danmaku] 弹幕记录列表
   */
  private def fetchDanmakuRecords()(using PlanContext): IO[List[Danmaku]] = {
    val sql =
      s"""
         |SELECT danmaku_id, content, video_id, author_id, danmaku_color, time_in_video
         |FROM $schemaName.danmaku_table
         |WHERE video_id = ?
         |ORDER BY time_in_video ASC;
       """.stripMargin
    for {
      _ <- IO(logger.info(s"执行SQL查询获取弹幕记录，SQL语句: $sql"))
      dbRows <- readDBRows(sql, List(SqlParameter("Int", videoID.toString)))
      danmakuRecords <- IO {
        dbRows.map(json => {
          Danmaku(
          danmakuID = decodeField[Int](json, "danmaku_id"),
          content = decodeField[String](json, "content"),
          videoID = decodeField[Int](json, "video_id"),
          authorID = decodeField[Int](json, "author_id"),
          danmakuColor = decodeField[String](json, "danmaku_color"),
          timeInVideo = decodeField[Float](json, "time_in_video")
        )}) // 将JSON解码为Danmaku对象
      }
      _ <- IO(logger.info(s"SQL查询返回 ${danmakuRecords.size} 条记录"))
    } yield danmakuRecords
  }
}