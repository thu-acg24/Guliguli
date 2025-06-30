package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class SearchVideosMessagePlanner(
    searchString: String,
    rangeL: Int,
    rangeR: Int,
    override val planContext: PlanContext
) extends Planner[Option[List[Video]]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[List[Video]]] = {
    logger.info(s"执行SearchVideosMessage，searchString=${searchString}, rangeL=${rangeL}, rangeR=${rangeR}")
    for {
      // Step 1: 查询匹配的 VideoInfoTable 中的 title 字段
      _ <- IO(logger.info("开始匹配 VideoInfoTable 中的 title 字段"))
      matchingVideos <- getMatchingVideos()

      // Step 2: 按 uploadTime 倒序排序
      _ <- IO(logger.info(s"查询到的匹配 Video 数量：${matchingVideos.length}"))
      sortedVideos <- IO {
        matchingVideos.sortBy(video => decodeField[DateTime](video, "upload_time").getMillis).reverse
      }

      // Step 3: 分页筛选
      _ <- IO(logger.info(s"开始分页处理：RangeL=${rangeL}, RangeR=${rangeR}"))
      paginatedVideos <- IO { sortedVideos.slice(rangeL, rangeR) }

      // Step 4: 组装为 Video 对象
      _ <- IO(logger.info("开始将查询结果转换为 Video 对象列表"))
      videoObjects <- IO { paginatedVideos.map(decodeType[Video]) }

      // Step 5: 返回最终结果
      _ <- IO(logger.info(s"返回的 Video 对象数量：${videoObjects.length}"))
    } yield if (videoObjects.isEmpty) Some(Nil) else Some(videoObjects)
  }.handleErrorWith { error =>
    logger.error(s"SearchVideosMessage 执行过程中发生错误: ${error.getMessage}")
    IO.pure(None)
  }

  private def getMatchingVideos()(using PlanContext): IO[List[Json]] = {
    val sql =
      s"""
         |SELECT *
         |FROM ${schemaName}.video_info_table
         |WHERE title ILIKE ? AND visible = ?
       """.stripMargin

    // 构造SQL参数
    val parameters = List(
      SqlParameter("String", s"%${searchString}%"), // 模糊匹配
      SqlParameter("Boolean", "true") // 仅选择对外可见的视频
    )

    logger.info(s"执行SQL查询匹配视频: sql=${sql}")
    readDBRows(sql, parameters)
  }
}