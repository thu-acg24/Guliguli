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

case class QueryFavoriteVideosMessagePlanner(
                                             userID: Int,
                                             override val planContext: PlanContext
                                           ) extends Planner[List[VideoAbstract]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[VideoAbstract]] = {
    for {
      _ <- IO(logger.info(s"[QueryFavoriteVideos] Querying favorite videos for userID: $userID"))
      favoriteVideos <- queryFavoriteVideos()
      _ <- IO(logger.info(s"[QueryFavoriteVideos] Found ${favoriteVideos.length} favorite videos"))
    } yield favoriteVideos
  }

  private def queryFavoriteVideos()(using PlanContext): IO[List[VideoAbstract]] = {
    val sql = s"""
      SELECT v.video_id, v.title, v.description, v.duration, v.cover,
             v.uploader_id, v.views, v.likes, v.favorites, v.status, v.upload_time
      FROM ${schemaName}.video_table v
      INNER JOIN ${schemaName}.favorite_record_table f ON v.video_id = f.video_id
      WHERE f.user_id = ? AND v.status = 'Approved'
      ORDER BY f.timestamp DESC;
    """

    readDBRows(sql, List(SqlParameter("Int", userID.toString)))
      .flatMap(jsonList => jsonList.traverse(decodeVideoAbstract))
  }
}
