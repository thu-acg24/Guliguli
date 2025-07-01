package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryPendingVideosMessagePlanner(
                                             token: String,
                                             override val planContext: PlanContext
                                           ) extends Planner[List[Video]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[Video]] = {
    for {
      _ <- IO(logger.info("[Step 1]: 校验Token和用户权限"))
      userRole <- QueryUserRoleMessage(token).send

      pendingVideos <- userRole match {
        case UserRole.Auditor =>
          IO(logger.info("[Step 2]: 查询待审核的视频")) >>
          fetchPendingVideos()
        case _ =>
          IO(logger.info("[Step 2.1]: 用户权限无效或者Token无效")) >>
          IO.raiseError(IllegalArgumentException("Invalid User Role"))
      }

    } yield pendingVideos
  }

  private def fetchPendingVideos()(using PlanContext): IO[List[Video]] = {
    val sql =
      s"""
        SELECT video_id, title, description, duration, tag, server_path, cover_path, uploader_id, views, likes, favorites, status, upload_time
        FROM ${schemaName}.video_table
        WHERE status = ?
        ORDER BY upload_time DESC;
      """
    val parameters = List(SqlParameter("String", VideoStatus.Pending.toString))

    for {
      _ <- IO(logger.info("[fetchPendingVideos]: 开始执行查询待审核的视频指令"))
      rows <- readDBRows(sql, parameters)
      result <- IO(rows.map(decodeType[Video]))
    } yield result
  }
}