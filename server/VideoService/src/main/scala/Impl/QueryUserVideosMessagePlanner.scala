package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.APIException.InvalidInputException
import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import Utils.DecodeVideo.decodeVideo
import cats.effect.IO
import cats.implicits.*
import cats.implicits.*
import io.circe.Json
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryUserVideosMessagePlanner(
                                         token: Option[String],
                                         userId: Int,
                                         override val planContext: PlanContext
                                       ) extends Planner[List[Video]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[Video]] = {
    for {
      _ <- IO(logger.info("[Step 1]: 验证Token和用户权限"))
      userInfo <- validateToken()
      
      _ <- IO(logger.info("[Step 2]: 查询用户视频列表"))
      videos <- queryUserVideos(userInfo)
    } yield videos
  }

  private def validateToken()(using PlanContext): IO[(Option[Int], Option[UserRole])] = {
    token match {
      case Some(tkn) =>
        for {
          _ <- IO(logger.info(s"[Step 1.1] 验证token: $tkn"))
          userID <- GetUIDByTokenMessage(tkn).send
          _ <- IO(logger.info(s"[Step 1.2] 获取到用户ID: $userID"))
          
          role <- QueryUserRoleMessage(tkn).send
          _ <- IO(logger.info(s"[Step 1.3] 获取到用户角色: $role"))
        } yield (Some(userID), Some(role))
      case None =>
        IO(logger.info("[Step 1.1] 未提供token，使用游客权限")) *> IO.pure((None, None))
    }
  }

  private def queryUserVideos(userInfo: (Option[Int], Option[UserRole]))(using PlanContext): IO[List[Video]] = {
    val (currentUserID, role) = userInfo
    
    // 确定查询条件：
    // 1. 如果是管理员或审核员，可以看到所有状态的视频
    // 2. 如果是查询自己的视频，可以看到自己的所有状态视频
    // 3. 如果是查询别人的视频，只能看到已审核通过的视频
    val (whereClause, parameters) = (currentUserID, role) match {
      case (Some(uid), Some(UserRole.Auditor)) =>
        // 审核员可以看到指定用户的所有视频
        ("WHERE uploader_id = ?", List(SqlParameter("Int", userId.toString)))
      case (Some(uid), _) if uid == userId =>
        // 查询自己的视频，可以看到所有状态
        ("WHERE uploader_id = ?", List(SqlParameter("Int", userId.toString)))
      case _ =>
        // 查询别人的视频，只能看到已审核通过的视频
        ("WHERE uploader_id = ? AND status = ?", 
         List(SqlParameter("Int", userId.toString), SqlParameter("String", VideoStatus.Approved.toString)))
    }
    
    val sql =
      s"""
        SELECT video_id, title, description, duration, cover,
             uploader_id, views, likes, favorites, status, upload_time
        FROM ${schemaName}.video_table
        $whereClause
        ORDER BY upload_time DESC;
      """

    readDBRows(sql, parameters)
      .flatMap(jsonList => jsonList.traverse(decodeVideo))
  }
}