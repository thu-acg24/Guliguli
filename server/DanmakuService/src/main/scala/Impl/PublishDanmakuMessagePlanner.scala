package Impl


import APIs.UserService.GetUIDByTokenMessage
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
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class PublishDanmakuMessagePlanner(
  token: String,
  videoID: Int,
  timeInVideo: Float,
  danmakuContent: String,
  danmakuColor: String,
  override val planContext: PlanContext
) extends Planner[Unit] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info("Step 1: 校验token合法性，并解析用户ID"))
      userID <- validateToken(token)
      _ <- IO(logger.info(s"userID为: $userID"))

      _ <- IO(logger.info("Step 2: 校验视频ID是否存在"))
      video <- validateVideoID(videoID)
      _ <- IO(logger.info(s"video信息为: $video"))

      _ <- IO(logger.info("Step 3: 校验弹幕颜色合法性"))
      colorValid <- validateDanmakuColor(danmakuColor)
      _ <- IO(logger.info(s"danmakuColor合法性: ${colorValid}"))

      _ <- if (!colorValid) {
        IO.raiseError(IllegalArgumentException("Color invalid"))
      } else {
        saveDanmaku(videoID, userID, timeInVideo, danmakuContent, danmakuColor)
      }
    } yield ()
  }

  private def validateToken(token: String)(using PlanContext): IO[Int] = {
    IO(logger.info(s"调用getUIDByTokenMessage校验token: ${token}")) >>
      GetUIDByTokenMessage(token).send
  }

  private def validateVideoID(videoID: Int)(using PlanContext): IO[Video] = {
    IO(logger.info(s"调用QueryVideoInfoMessage校验videoID: ${videoID}")) >>
      QueryVideoInfoMessage(None, videoID).send
  }

  private def validateDanmakuColor(color: String)(using PlanContext): IO[Boolean] = {
    IO(logger.info(s"校验弹幕颜色: ${color}")) >>
      IO(color.matches("^#[0-9a-fA-F]{6}$"))
  }

  private def saveDanmaku(videoID: Int, userID: Int, timeInVideo: Float, content: String, color: String)(using PlanContext): IO[String] = {
    IO(logger.info("Step 4: 插入弹幕数据到数据库")) >>
      {
        val sql =
          s"""
             |INSERT INTO $schemaName.danmaku_table
             |(content, video_id, author_id, danmaku_color, time_in_video, timestamp)
             |VALUES (?, ?, ?, ?, ?, ?)
             |""".stripMargin

        val params = List(
          SqlParameter("String", content),
          SqlParameter("Int", videoID.toString),
          SqlParameter("Int", userID.toString),
          SqlParameter("String", color),
          SqlParameter("Float", timeInVideo.toString),
          SqlParameter("DateTime", DateTime.now().getMillis.toString)
        )

        writeDB(sql, params)
      }
  }
}