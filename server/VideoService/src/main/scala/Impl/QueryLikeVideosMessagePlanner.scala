package Impl

import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.ServiceUtils.schemaName
import Objects.VideoService.VideoAbstract
import Utils.DecodeVideo.decodeVideoAbstract
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.slf4j.LoggerFactory

case class QueryLikeVideosMessagePlanner(
                                         userID: Int,
                                         override val planContext: PlanContext
                                       ) extends Planner[List[VideoAbstract]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[VideoAbstract]] = {
    for {
      _ <- IO(logger.info(s"[QueryLikeVideos] Querying liked videos for userID: $userID"))
      likedVideos <- queryLikedVideos()
      _ <- IO(logger.info(s"[QueryLikeVideos] Found ${likedVideos.length} liked videos"))
    } yield likedVideos
  }

  private def queryLikedVideos()(using PlanContext): IO[List[VideoAbstract]] = {
    val sql = s"""
      SELECT v.video_id, v.title, v.description, v.duration, v.cover,
             v.uploader_id, v.views, v.likes, v.favorites, v.status, v.upload_time
      FROM ${schemaName}.video_table v
      INNER JOIN ${schemaName}.like_record_table l ON v.video_id = l.video_id
      WHERE l.user_id = ? AND v.status = 'Approved'
      ORDER BY l.timestamp DESC;
    """

    readDBRows(sql, List(SqlParameter("Int", userID.toString)))
      .flatMap(jsonList => jsonList.traverse(decodeVideoAbstract))
  }
}
