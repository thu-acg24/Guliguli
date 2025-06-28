package Impl


import Objects.VideoService.VideoStatus
import Objects.VideoService.Video
import Objects.UserService.UserRole
import APIs.UserService.QueryUserRoleMessage
import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import org.joda.time.DateTime
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import cats.implicits._
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
import APIs.UserService.getUIDByTokenMessage
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryPendingVideosMessagePlanner(
                                             token: String,
                                             override val planContext: PlanContext
                                           ) extends Planner[Option[List[Video]]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[List[Video]]] = {
    for {
      _ <- IO(logger.info("[Step 1]: 校验Token和用户权限"))
      userRoleOpt <- checkUserPermission(token)

      _ <- IO(logger.info("[Step 2]: 查询待审核的视频"))
      pendingVideosOpt <- userRoleOpt match {
        case Some(UserRole.Auditor) =>
          fetchPendingVideos()
        case _ =>
          IO(logger.info("[Step 2.1]: 用户权限无效或者Token无效，返回None")) *> IO(None)
      }

    } yield pendingVideosOpt
  }

  private def checkUserPermission(token: String)(using PlanContext): IO[Option[UserRole]] = {
    for {
      _ <- IO(logger.info(s"[checkUserPermission]: 校验Token：${token}"))
      uidOpt <- getUIDByTokenMessage(token).send

      userRoleOpt <- uidOpt match {
        case Some(uid) =>
          IO(logger.info(s"[checkUserPermission]: Token有效, 对应userID=${uid}, 开始校验用户权限")) >>
          QueryUserRoleMessage(token).send
        case None =>
          IO(logger.info("[checkUserPermission]: Token无效，返回None")) *> IO(None)
      }
    } yield userRoleOpt
  }

  private def fetchPendingVideos()(using PlanContext): IO[Option[List[Video]]] = {
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
      resultOpt <- IO {
        if (rows.isEmpty) {
          logger.info("[fetchPendingVideos]: 查询结果为空，返回None")
          None
        } else {
          logger.info(s"[fetchPendingVideos]: 查询成功，共找到${rows.size}条记录")
          Some(rows.map(decodeType[Video]))
        }
      }
    } yield resultOpt
  }
}