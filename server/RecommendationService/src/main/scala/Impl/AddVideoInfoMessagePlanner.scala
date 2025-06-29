package Impl


import Objects.RecommendationService.VideoInfo
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import APIs.VideoService.QueryVideoInfoMessage
import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import cats.implicits.*
import io.circe.syntax.*
import io.circe._
import io.circe.generic.auto._
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
import io.circe.syntax._
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class AddVideoInfoMessagePlanner(
    token: String,
    info: VideoInfo,
    override val planContext: PlanContext
) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // 主函数计划
  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: 校验Token是否合法并获取用户的userID
      _ <- IO(logger.info(s"[Step 1] 校验Token是否合法并获取用户的userID: ${token}"))
      userIDOption <- GetUIDByTokenMessage(token).send
      userID <- validateUserID(userIDOption)

      // Step 2: 校验用户是否为当前视频的上传者
      _ <- IO(logger.info(s"[Step 2] 校验用户是否为视频ID [${info.videoID}] 的上传者"))
      _ <- validateUploader(userID)

      // Step 3: 在VideoInfoTable中创建新的视频信息记录
      _ <- IO(logger.info(s"[Step 3] 准备在VideoInfoTable中插入视频信息: ${info}"))
      insertResult <- insertVideoInfo()

      // Step 4: 返回结果
      _ <- IO(logger.info(s"[Step 4] 操作完成，返回结果: ${insertResult.getOrElse("Success")}"))
    } yield insertResult
  }

  // 子函数：校验用户ID
  private def validateUserID(userIDOption: Option[Int])(using PlanContext): IO[Int] = {
    userIDOption match {
      case Some(userID) =>
        IO.pure(userID)
      case None =>
        IO.pure(throw new IllegalArgumentException("Invalid Token"))
    }
  }

  // 子函数：校验用户是否为上传者
  private def validateUploader(userID: Int)(using PlanContext): IO[Unit] = {
    for {
      videoOption <- QueryVideoInfoMessage(None, info.videoID).send
      _ <- videoOption match {
        case Some(video) if video.uploaderID == userID =>
          IO(logger.info(s"用户${userID}是视频ID [${info.videoID}] 的上传者"))
        case Some(_) =>
          IO.raiseError(new IllegalArgumentException("Permission Denied"))
        case None =>
          IO.raiseError(new IllegalArgumentException("Video Not Found"))
      }
    } yield ()
  }

  // 子函数：插入视频信息记录
  private def insertVideoInfo()(using PlanContext): IO[Option[String]] = {
    val sql =
      s"""
        INSERT INTO ${schemaName}.video_info_table
        (video_id, title, description, tag, uploader_id, views, likes, favorites, visible)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
      """
    val parameters = List(
      SqlParameter("Int", info.videoID.toString),
      SqlParameter("String", info.title),
      SqlParameter("String", info.description),
      SqlParameter("Array[String]", info.tag.asJson.noSpaces),
      SqlParameter("Int", info.uploaderID.toString),
      SqlParameter("Int", info.views.toString),
      SqlParameter("Int", info.likes.toString),
      SqlParameter("Int", info.favorites.toString),
      SqlParameter("Boolean", info.visible.toString)
    )
    writeDB(sql, parameters).map { result =>
      if (result == "Operation(s) done successfully")
        None // 插入成功
      else
        Some("Unable to add video information")
    }
  }
}