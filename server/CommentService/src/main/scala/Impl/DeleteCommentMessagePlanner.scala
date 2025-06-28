package Impl


import APIs.UserService.{QueryUserRoleMessage, getUIDByTokenMessage}
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Objects.VideoService.Video
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Objects.VideoService.VideoStatus
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
import APIs.UserService.QueryUserRoleMessage
import APIs.UserService.getUIDByTokenMessage
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import APIs.UserService.getUIDByTokenMessage

case class DeleteCommentMessagePlanner(
                                         token: String,
                                         commentID: Int,
                                         override val planContext: PlanContext
                                       ) extends Planner[Option[String]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: 校验token是否有效并获取用户ID
      _ <- IO(logger.info(s"Step 1: 验证token[$token]是否合法"))
      userIDOption <- getUserID(token)
      result <- userIDOption match {
        case None =>
          IO(logger.info("token无效, 返回Some('Invalid Token')")) *> IO(Some("Invalid Token"))
        case Some(userID) =>
          // Step 2: 校验commentID是否存在
          validateCommentExistsAndFetchAuthor(commentID).flatMap {
            case None =>
              IO(logger.info("commentID不存在, 返回Some('Comment Not Found')")) *> IO(Some("Comment Not Found"))
            case Some((authorID, videoID)) =>
              // Step 3: 检查用户身份是否符合删除权限
              checkUserPermission(userID, authorID, videoID).flatMap {
                case false =>
                  IO(logger.info("无权限删除, 返回Some('Permission Denied')")) *> IO(Some("Permission Denied"))
                case true =>
                  // Step 4: 从CommentTable中删除记录
                  deleteComment(commentID).flatMap {
                    case false =>
                      IO(logger.info("删除失败, 返回Some('Failed to delete comment')")) *> IO(Some("Failed to delete comment"))
                    case true =>
                      IO(logger.info("删除成功, 返回None")) *> IO(None)
                  }
              }
          }
      }
    } yield result
  }

  private def getUserID(token: String)(using PlanContext): IO[Option[Int]] = {
    getUIDByTokenMessage(token).send
  }

  private def validateCommentExistsAndFetchAuthor(commentID: Int)(using PlanContext): IO[Option[(Int, Int)]] = {
    logger.info(s"验证评论是否存在并获取作者ID和对应视频ID, commentID: ${commentID}")
    val sql =
      s"""
SELECT author_id, video_id
FROM ${schemaName}.comment_table
WHERE comment_id = ?;
""".stripMargin

    readDBJsonOptional(sql, List(SqlParameter("Int", commentID.toString))).map {
      case None => None
      case Some(json) =>
        val authorID = decodeField[Int](json, "author_id")
        val videoID = decodeField[Int](json, "video_id")
        Some((authorID, videoID))
    }
  }

  private def checkUserPermission(userID: Int, authorID: Int, videoID: Int)(using PlanContext): IO[Boolean] = {
    logger.info(s"检查用户[userID: ${userID}]的删除权限")
    if (userID == authorID) {
      // 用户是评论的作者
      IO(logger.info("用户是评论的作者，允许删除")) *> IO(true)
    } else {
      // 检查用户是否是视频的发布者
      val videoPermissionCheck =
        QueryVideoInfoMessage(None, videoID).send.map {
          case None =>
            logger.info("视频信息不存在，权限校验失败")
            false
          case Some(video) =>
            if (video.uploaderID == userID) {
              logger.info("用户是视频的发布者，允许删除")
              true
            } else {
              logger.info("用户不是视频的发布者")
              false
            }
        }

      // 检查用户是否是审核员
      val roleCheck =
        QueryUserRoleMessage(token).send.map {
          case Some(role) if role == UserRole.Auditor =>
            logger.info("用户角色是审核员，允许删除")
            true
          case _ =>
            logger.info("用户不是审核员")
            false
        }

      for {
        isUploader <- videoPermissionCheck
        isAuditor <- roleCheck
      } yield isUploader || isAuditor
    }
  }

  private def deleteComment(commentID: Int)(using PlanContext): IO[Boolean] = {
    logger.info(s"从CommentTable中删除记录, commentID: ${commentID}")
    val sql =
      s"""
DELETE FROM ${schemaName}.comment_table
WHERE comment_id = ?;
""".stripMargin

    writeDB(sql, List(SqlParameter("Int", commentID.toString)))
      .attempt.map {
        case Left(e) =>
          logger.error("删除CommentTable记录失败", e)
          false
        case Right(_) =>
          logger.info("成功删除CommentTable记录")
          true
      }
  }
}