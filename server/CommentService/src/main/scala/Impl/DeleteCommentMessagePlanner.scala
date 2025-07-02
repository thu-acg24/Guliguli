package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.APIException.InvalidInputException
import APIs.UserService.QueryUserRoleMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.UserService.UserRole
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class DeleteCommentMessagePlanner(
                                         token: String,
                                         commentID: Int,
                                         override val planContext: PlanContext
                                       ) extends Planner[Unit] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: 校验token是否有效并获取用户ID
      _ <- IO(logger.info(s"Step 1: 验证token[$token]是否合法"))
      userID <- getUserID(token)
      // Step 2: 校验commentID是否存在
      (authorID, videoID) <- validateCommentExistsAndFetchAuthor(commentID)
      // Step 3: 检查用户身份是否符合删除权限
      _ <- checkUserPermission(userID, authorID, videoID)
      // Step 4: 从CommentTable中删除记录
      _ <- deleteComment(commentID)
    } yield ()
  }

  private def getUserID(token: String)(using PlanContext): IO[Int] = {
    GetUIDByTokenMessage(token).send
  }

  private def validateCommentExistsAndFetchAuthor(commentID: Int)(using PlanContext): IO[(Int, Int)] = {
    logger.info(s"验证评论是否存在并获取作者ID和对应视频ID, commentID: ${commentID}")
    val sql =
      s"""
SELECT author_id, video_id
FROM ${schemaName}.comment_table
WHERE comment_id = ?;
""".stripMargin

    readDBJsonOptional(sql, List(SqlParameter("Int", commentID.toString))).map {
      case None => throw InvalidInputException("视频不存在")
      case Some(json) =>
        val authorID = decodeField[Int](json, "author_id")
        val videoID = decodeField[Int](json, "video_id")
        (authorID, videoID)
    }
  }

  private def checkUserPermission(userID: Int, authorID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    logger.info(s"检查用户[userID: ${userID}]的删除权限")
    if (userID == authorID) {
      // 用户是评论的作者
      IO(logger.info("用户是评论的作者，允许删除")) *> IO(true)
    } else {
      // 检查用户是否是视频的发布者
      val videoPermissionCheck =
        QueryVideoInfoMessage(None, videoID).send.flatMap { video =>
          if (video.uploaderID == userID) {
            IO(logger.info("用户是视频的发布者，允许删除")) >>
            IO.pure(true)
          } else {
            IO(logger.info("用户不是视频的发布者")) >>
            IO.pure(false)
          }
        }

      // 检查用户是否是审核员
      val roleCheck =
        QueryUserRoleMessage(token).send.flatMap {
          case role if role == UserRole.Auditor =>
            IO(logger.info("用户角色是审核员，允许删除")) >>
            IO.pure(true)
          case _ =>
            IO(logger.info("用户不是审核员")) >>
            IO.pure(false)
        }

      for {
        isUploader <- videoPermissionCheck
        isAuditor <- roleCheck
        _ <- IO.unit.ensure(new InvalidInputException("Permission denied")) { _ =>
          isUploader || isAuditor
        }
      } yield ()
    }
  }

  private def deleteComment(commentID: Int)(using PlanContext): IO[String] = {
    logger.info(s"从CommentTable中删除记录, commentID: ${commentID}")
    val sql =
      s"""
DELETE FROM ${schemaName}.comment_table
WHERE comment_id = ?;
""".stripMargin

    writeDB(sql, List(SqlParameter("Int", commentID.toString)))
  }
}