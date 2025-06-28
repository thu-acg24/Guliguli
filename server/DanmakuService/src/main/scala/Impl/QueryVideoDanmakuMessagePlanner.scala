package Impl


import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import APIs.VideoService.QueryVideoInfoMessage
import Objects.DanmakuService.Danmaku
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
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
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryVideoDanmakuMessagePlanner(videoID: Int, token: Option[String], override val planContext: PlanContext) extends Planner[Option[List[Danmaku]]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[List[Danmaku]]] = {
    for {
      _ <- IO(logger.info(s"校验视频ID ${videoID} 是否合法"))
      videoOpt <- validateVideoExistence()
      _ <- IO(logger.info(s"校验结果: ${videoOpt.isDefined}"))

      result <- videoOpt match {
        case None =>
          IO(logger.info(s"视频ID ${videoID} 无效, 返回None")) >> IO.pure(None)
        case Some(_) =>
          for {
            _ <- IO(logger.info(s"视频ID ${videoID} 有效，查询相关弹幕记录"))
            danmakuRecords <- fetchDanmakuRecords()
            _ <- IO(logger.info(s"查询到 ${danmakuRecords.size} 条弹幕记录"))

            sortedRecords <- IO(logger.info("根据timeInVideo字段对弹幕记录排序")) >> sortDanmakuRecords(danmakuRecords)
            _ <- IO(logger.info(s"排序完成，共有 ${sortedRecords.size} 条弹幕记录"))

            result = Some(sortedRecords)
            _ <- IO(logger.info(s"返回封装后的弹幕查询结果"))
          } yield result
      }
    } yield result
  }

  /**
   * 校验视频是否存在
   * @return Option[Video] 如果视频存在, 返回视频信息; 否则返回None
   */
  private def validateVideoExistence()(using PlanContext): IO[Option[Video]] = {
    QueryVideoInfoMessage(token, videoID).send
  }

  /**
   * 查询与视频关联的弹幕记录
   * @return List[Danmaku] 弹幕记录列表
   */
  private def fetchDanmakuRecords()(using PlanContext): IO[List[Danmaku]] = {
    val sql =
      s"""
         |SELECT danmaku_id, content, video_id, author_id, danmaku_color, time_in_video
         |FROM ${schemaName}.danmaku_table
         |WHERE video_id = ?;
       """.stripMargin
    for {
      _ <- IO(logger.info(s"执行SQL查询获取弹幕记录，SQL语句: $sql"))
      dbRows <- readDBRows(sql, List(SqlParameter("Int", videoID.toString)))
      danmakuRecords <- IO {
        dbRows.map(decodeType[Danmaku]) // 将JSON解码为Danmaku对象
      }
      _ <- IO(logger.info(s"SQL查询返回 ${danmakuRecords.size} 条记录"))
    } yield danmakuRecords
  }

  /**
   * 根据timeInVideo字段排序弹幕记录
   * @param records 未排序的弹幕记录列表
   * @return 排序后的弹幕记录列表
   */
  private def sortDanmakuRecords(records: List[Danmaku]): IO[List[Danmaku]] = {
    IO(records.sortBy(_.timeInVideo))
  }
}