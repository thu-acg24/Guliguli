package Impl


import Objects.VideoService.{Video, VideoStatus}
import Objects.DanmakuService.Danmaku
import APIs.VideoService.QueryVideoInfoMessage
import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits._
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
import Objects.VideoService.VideoStatus
import Objects.VideoService.Video
import APIs.UserService.getUIDByTokenMessage
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class PublishDanmakuMessagePlanner(
  token: String,
  videoID: Int,
  timeInVideo: Float,
  danmakuContent: String,
  danmakuColor: String,
  override val planContext: PlanContext
) extends Planner[Option[String]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info("Step 1: 校验token合法性，并解析用户ID"))
      userIDOpt <- validateToken(token)
      _ <- IO(logger.info(s"userID为: ${userIDOpt.getOrElse("None")}"))
      result <- userIDOpt match {
        case None => IO.pure(Some("Invalid Token"))
        case Some(userID) =>
          for {
            _ <- IO(logger.info("Step 2: 校验视频ID是否存在"))
            videoOpt <- validateVideoID(videoID)
            _ <- IO(logger.info(s"video信息为: ${videoOpt.map(_.title).getOrElse("None")}"))
            stepResult <- videoOpt match {
              case None => IO.pure(Some("Video Not Found"))
              case Some(video) =>
                for {
                  _ <- IO(logger.info("Step 3: 校验弹幕颜色合法性"))
                  colorValid <- validateDanmakuColor(danmakuColor)
                  _ <- IO(logger.info(s"danmakuColor合法性: ${colorValid}"))
                  finalResult <- if (!colorValid) IO.pure(Some("Color invalid")) else saveDanmaku(videoID, userID, timeInVideo, danmakuContent, danmakuColor)
                } yield finalResult
            }
          } yield stepResult
      }
    } yield result
  }

  private def validateToken(token: String)(using PlanContext): IO[Option[Int]] = {
    IO(logger.info(s"调用getUIDByTokenMessage校验token: ${token}")) >>
      getUIDByTokenMessage(token).send
  }

  private def validateVideoID(videoID: Int)(using PlanContext): IO[Option[Video]] = {
    IO(logger.info(s"调用QueryVideoInfoMessage校验videoID: ${videoID}")) >>
      QueryVideoInfoMessage(None, videoID).send
  }

  private def validateDanmakuColor(color: String)(using PlanContext): IO[Boolean] = {
    IO(logger.info(s"校验弹幕颜色: ${color}")) >>
      IO(color.matches("^#[0-9a-fA-F]{6}$"))
  }

  private def saveDanmaku(videoID: Int, userID: Int, timeInVideo: Float, content: String, color: String)(using PlanContext): IO[Option[String]] = {
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

        writeDB(sql, params).map(_ => None).handleError { ex =>
          logger.error(s"弹幕发布失败: ${ex.getMessage}")
          Some("Failed to publish danmaku")
        }
      }
  }
}