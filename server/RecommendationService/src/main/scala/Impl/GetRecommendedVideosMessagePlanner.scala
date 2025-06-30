package Impl


import APIs.HistoryService.QueryHistoryMessage
import APIs.UserService.QueryUserInfoMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.HistoryService.HistoryRecord
import Objects.UserService.UserInfo
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class GetRecommendedVideosMessagePlanner(
    videoID: Option[Int],
    userID: Option[Int],
    override val planContext: PlanContext
) extends Planner[List[Video]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[Video]] = {
    for {
      // Step 1: Validate input parameters
      _ <- IO(logger.info(s"校验入参，确保 videoID 或 userID 至少有一个有效值"))
      _ <- validateInputParams

      // Step 2: Generate recommended video IDs
      _ <- IO(logger.info(s"根据用户行为或视频标签生成推荐视频ID列表"))
      recommendedVideoIDs <- generateRecommendedVideoIDs

      // Step 3: Fetch full video details
      _ <- IO(logger.info(s"将推荐视频ID列表映射为完整视频对象"))
      recommendedVideos <- fetchVideoDetails(recommendedVideoIDs)
    } yield recommendedVideos
  }

  private def validateInputParams(using PlanContext): IO[Unit] = {
    if (videoID.isEmpty && userID.isEmpty)
      IO.raiseError(new IllegalArgumentException("videoID 和 userID 至少需要一个有效值"))
    else
      for {
        _ <- videoID match {
          case Some(id) =>
            IO(logger.info(s"验证视频ID ${id} 是否有效"))
              .flatMap(_ => validateVideoID(id))
          case None => IO.unit
        }
        _ <- userID match {
          case Some(id) =>
            IO(logger.info(s"验证用户ID ${id} 是否有效"))
              .flatMap(_ => validateUserID(id))
          case None => IO.unit
        }
      } yield ()
  }

  private def validateVideoID(id: Int)(using PlanContext): IO[Unit] = {
    QueryVideoInfoMessage(None, id).send.flatMap {
      case Some(_) =>
        IO(logger.info(s"视频ID ${id} 验证通过"))
      case None =>
        IO.raiseError(new IllegalArgumentException(s"无效的视频ID：${id}"))
    }
  }

  private def validateUserID(id: Int)(using PlanContext): IO[Unit] = {
    QueryUserInfoMessage(id).send.flatMap {
      case Some(_) =>
        IO(logger.info(s"用户ID ${id} 验证通过"))
      case None =>
        IO.raiseError(new IllegalArgumentException(s"无效的用户ID：${id}"))
    }
  }

  private def generateRecommendedVideoIDs(using PlanContext): IO[List[Int]] = {
    userID match {
      case Some(uid) =>
        IO(logger.info(s"基于用户ID ${uid} 的观看历史生成推荐视频"))
          .flatMap(_ => recommendByUserID(uid))
      case None =>
        videoID match {
          case Some(vid) =>
            IO(logger.info(s"基于视频ID ${vid} 的标签信息生成推荐视频"))
              .flatMap(_ => recommendByVideoID(vid))
          case None =>
            IO.raiseError(new IllegalStateException("用户ID和视频ID都缺失，不可能生成推荐列表"))
        }
    }
  }

  private def recommendByUserID(userID: Int)(using PlanContext): IO[List[Int]] = {
    QueryHistoryMessage("dummyToken", 0, 10).send.flatMap {
      case Some(history) =>
        IO(logger.info(s"用户观看历史获取成功，包含 ${history.length} 条记录"))
          .flatMap(_ => analyzeUserHistory(history))
      case None =>
        IO(logger.info(s"用户 ${userID} 没有观看历史，返回空的推荐列表")).map(_ => List.empty)
    }
  }

  private def analyzeUserHistory(history: List[HistoryRecord]): IO[List[Int]] = {
    // 数据分析的伪逻辑，这里需要调用某种推荐模型或者算法
    val videoIDs = history.map(_.videoID)
    IO(logger.info(s"基于观看历史生成的推荐视频ID：${videoIDs}")).map(_ => videoIDs)
  }

  private def recommendByVideoID(videoID: Int)(using PlanContext): IO[List[Int]] = {
    val sql =
      s"""
SELECT tag
FROM ${schemaName}.video_info_table
WHERE video_id = ? AND visible = true;
""".stripMargin

    readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString))).flatMap {
      case Some(json) =>
        val tags = decodeField[List[String]](json, "tag")
        IO(logger.info(s"视频ID ${videoID} 的标签信息为：${tags}"))
          .flatMap(_ => generateRecommendationsFromTags(tags))
      case None =>
        IO(logger.info(s"未能找到视频ID ${videoID} 的标签信息，返回空推荐列表")).map(_ => List.empty)
    }
  }

  private def generateRecommendationsFromTags(tags: List[String]): IO[List[Int]] = {
    val dummyRecommendations = tags.flatMap(tag => List(tag.hashCode.abs % 100)) // 简化示例
    IO(logger.info(s"基于标签生成的推荐视频ID：${dummyRecommendations}")).map(_ => dummyRecommendations)
  }

  private def fetchVideoDetails(videoIDs: List[Int])(using PlanContext): IO[List[Video]] = {
    videoIDs match {
      case Nil => IO.pure(List.empty)
      case ids =>
        IO(logger.info(s"开始根据ID获取视频完整信息，共 ${ids.length} 个")) *>
          ids.traverse(fetchSingleVideo)
    }
  }

  private def fetchSingleVideo(videoID: Int)(using PlanContext): IO[Video] = {
    QueryVideoInfoMessage(None, videoID).send.flatMap {
      case Some(video) => IO.pure(video)
      case None =>
        IO.raiseError(new IllegalStateException(s"无法获取视频ID ${videoID} 的详情"))
    }
  }
}