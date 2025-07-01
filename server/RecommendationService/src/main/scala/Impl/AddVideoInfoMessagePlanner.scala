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
import Objects.RecommendationService.VideoInfo
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax.*
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class AddVideoInfoMessagePlanner(
    token: String,
    info: VideoInfo,
    override val planContext: PlanContext
) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // 主函数计划
  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: 校验Token是否合法并获取用户的userID
      _ <- IO(logger.info(s"[Step 1] 校验Token是否合法并获取用户的userID: ${token}"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: 校验用户是否为当前视频的上传者
      _ <- IO(logger.info(s"[Step 2] 校验用户是否为视频ID [${info.videoID}] 的上传者"))
      _ <- validateUploader(userID)

      // Step 3: 在VideoInfoTable中创建新的视频信息记录
      _ <- IO(logger.info(s"[Step 3] 准备在VideoInfoTable中插入视频信息: ${info}"))
      insertResult <- insertVideoInfo()

      // Step 4: 返回结果
      _ <- IO(logger.info("[Step 4] 操作完成，返回结果"))
    } yield Unit
  }

  // 子函数：校验用户是否为上传者
  private def validateUploader(userID: Int)(using PlanContext): IO[Unit] = {
    for{
      video <- QueryVideoInfoMessage(None, info.videoID).send
      _ <- if (video.uploaderID == userID) {
        IO(logger.info(s"用户${userID}是视频ID [${info.videoID}] 的上传者"))
      }else{
        IO.raiseError(IllegalArgumentException("User Are Not The Uploader Of Video"))
      }
    } yield Unit
  }

  // 子函数：插入视频信息记录
  private def insertVideoInfo()(using PlanContext): IO[Unit] = {
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
    writeDB(sql, parameters).as(())
  }
}