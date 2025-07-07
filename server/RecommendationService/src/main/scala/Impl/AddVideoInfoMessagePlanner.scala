package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.APIException.InvalidInputException
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.PGVector
import Objects.RecommendationService.VideoInfo
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import Utils.PerferenceProcess.getInfo
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.UUID

case class AddVideoInfoMessagePlanner(
    token: String,
    videoID: Int,
    override val planContext: PlanContext
) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // 主函数计划
  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: 校验Token是否合法并获取用户的userID
      _ <- IO(logger.info(s"[Step 1] 校验Token是否合法并获取用户的userID: ${token}"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: 获取视频
      _ <- IO(logger.info(s"[Step 2] 获取视频中"))
      video <- QueryVideoInfoMessage(Some(token), videoID).send

      // Step 3: 在VideoInfoTable中创建新的视频信息记录
      _ <- IO(logger.info(s"[Step 3] 准备在VideoInfoTable中插入视频信息:"))
      insertResult <- insertVideoInfo(video)

      // Step 4: 返回结果
      _ <- IO(logger.info("[Step 4] 操作完成，返回结果"))
    } yield ()
  }

  // 子函数：插入视频信息记录
  private def insertVideoInfo(video: Video)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
        INSERT INTO ${schemaName}.video_info_table
        (video_id, title, visible, embedding)
        VALUES (?, ?, ?, ?::vector);
      """
    val parameters = List(
      SqlParameter("Int", video.videoID.toString),
      SqlParameter("String", video.title + video.description),
      SqlParameter("Boolean", (video.status == VideoStatus.Approved).toString),
      SqlParameter("Vector", getInfo(video.tag).toString),
    )
    writeDB(sql, parameters).as(())
  }
}