package Impl

import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.ServiceUtils.schemaName
import Objects.VideoService.Video
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.slf4j.LoggerFactory

case class QueryLikeVideosMessagePlanner(
                                         userID: Int,
                                         override val planContext: PlanContext
                                       ) extends Planner[List[Video]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[Video]] = {
    for {
      _ <- IO(logger.info(s"[QueryLikeVideos] Querying liked videos for userID: $userID"))
      likedVideos <- queryLikedVideos()
      _ <- IO(logger.info(s"[QueryLikeVideos] Found ${likedVideos.length} liked videos"))
    } yield likedVideos
  }

  private def queryLikedVideos()(using PlanContext): IO[List[Video]] = {
    val sql = s"""
      SELECT v.video_id, v.title, v.description, v.duration, v.tag,  v.m3u8_name, v.ts_prefix, v.slice_count,
             v.uploader_id, v.views, v.likes, v.favorites, v.status, v.upload_time
      FROM ${schemaName}.video_table v
      INNER JOIN ${schemaName}.like_record_table l ON v.video_id = l.video_id
      WHERE l.user_id = ? AND v.status = 'Approved'
      ORDER BY l.timestamp DESC;
    """
    
    readDBRows(sql, List(SqlParameter("Int", userID.toString))).map { jsonList =>
      jsonList.map { json =>
        decodeType[Video](json)
      }
    }
  }
}
