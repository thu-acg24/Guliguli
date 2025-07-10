package Impl

import APIs.UserService.{GetUIDByTokenMessage, QueryFollowingListMessage, QueryUserRoleMessage, QueryUserStatMessage}
import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Objects.VideoService.{Video, VideoStatus}
import Utils.DecodeVideo.decodeVideo
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryFollowingVideosMessagePlanner(
                                         token: String,
                                         fetchLimit: Int,
                                         lastTime: DateTime,
                                         lastID: Int,
                                         override val planContext: PlanContext
                                       ) extends Planner[List[Video]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[Video]] = {
    for {
      _ <- IO(logger.info("[Step 1]: 验证Token和用户权限"))
      _ <- IO(logger.info(s"[Step 1.1] 验证token: $token"))
      userID <- GetUIDByTokenMessage(token).send
      _ <- IO(logger.info(s"[Step 1.2] 获取到用户ID: $userID"))

      role <- QueryUserRoleMessage(token).send
      _ <- IO(logger.info(s"[Step 1.3] 获取到用户角色: $role"))

      followingCount <- QueryUserStatMessage(userID).send.flatMap(stat => IO.pure(stat.followingCount))
      followingList <- QueryFollowingListMessage(userID, 1, followingCount + 1).send
        .flatMap(_.traverse(relation => IO.pure(relation.followeeID)))
      
      _ <- IO(logger.info("[Step 2]: 查询用户视频列表"))
      videos <- queryUserVideos(role, followingList)
    } yield videos
  }

  private def queryUserVideos(role: UserRole, IDList: List[Int])(using PlanContext): IO[List[Video]] = {
    
    // 确定查询条件：
    // 1. 如果是管理员或审核员，可以看到所有状态的视频
    // 2. 如果是查询自己的视频，可以看到自己的所有状态视频
    // 3. 如果是查询别人的视频，只能看到已审核通过的视频
    val (whereClause, parameters) = role match {
      case UserRole.Auditor =>
        // 审核员可以看到指定用户的所有视频
        ("WHERE ", List())
      case _ =>
        // 查询别人的视频，只能看到已审核通过的视频
        ("WHERE status = ? AND ", List(SqlParameter("String", VideoStatus.Approved.toString)))
    }
    
    val sql =
      s"""
        SELECT video_id, title, description, duration, tag, cover,
             uploader_id, views, likes, favorites, status, upload_time
        FROM $schemaName.video_table
        $whereClause uploader_id = ANY(?) AND (upload_time > ? OR (upload_time == ? AND video_id < ?))
        ORDER BY upload_time DESC, video_id DESC;
        LIMIT $fetchLimit
      """

    readDBRows(sql,
      parameters ::: List(
        SqlParameter("List[Int]", s"[${IDList.mkString(",")}]"),
        SqlParameter("DateTime", lastTime.toString),
        SqlParameter("DateTime", lastTime.toString),
        SqlParameter("Int", lastID.toString)
      )).flatMap(jsonList => jsonList.traverse(decodeVideo))
  }
}