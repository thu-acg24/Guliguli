package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import Objects.UserService.UserRole
import APIs.UserService.GetUIDByTokenMessage
import APIs.VideoService.QueryVideoInfoMessage
import APIs.UserService.QueryUserRoleMessage
import cats.effect.IO
import org.slf4j.LoggerFactory
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
import APIs.UserService.GetUIDByTokenMessage

case class DeleteDanmakuMessagePlanner(
                                        token: String,
                                        danmakuID: Int,
                                        override val planContext: PlanContext
                                      ) extends Planner[Option[String]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"开始验证Token并获取用户ID"))
      maybeUserID <- verifyToken(token)

      userID <- IO(maybeUserID.getOrElse {
        logger.error(s"无效的Token：${token}")
        return IO.pure(Some("Invalid Token"))
      })

      _ <- IO(logger.info(s"验证弹幕记录是否存在，弹幕ID：${danmakuID}"))
      maybeDanmakuRecord <- checkDanmakuExistence(danmakuID)

      (videoID, senderID) <- IO(maybeDanmakuRecord.getOrElse {
        logger.error(s"弹幕记录不存在，弹幕ID：${danmakuID}")
        return IO.pure(Some("Danmaku Not Found"))
      })

      _ <- IO(logger.info(s"校验用户删除权限"))
      hasPermission <- checkDeletionPermission(userID, videoID, senderID, token)

      _ <- if (!hasPermission) {
        IO(logger.error(s"用户无删除权限，用户ID：${userID}, 弹幕ID：${danmakuID}"))
        return IO.pure(Some("Unauthorized Action"))
      } else IO.unit

      _ <- IO(logger.info(s"开始删除弹幕记录，弹幕ID：${danmakuID}"))
      deletionResult <- deleteDanmakuRecord(danmakuID)

      result <- if (deletionResult) {
        IO(logger.info(s"弹幕记录删除成功，弹幕ID：${danmakuID}"))
        IO.pure(None)
      } else {
        IO(logger.error(s"删除弹幕记录失败，弹幕ID：${danmakuID}"))
        IO.pure(Some("Failed to delete danmaku"))
      }
    } yield result
  }

  private def verifyToken(token: String)(using PlanContext): IO[Option[Int]] = {
    GetUIDByTokenMessage(token).send
  }

  private def checkDanmakuExistence(danmakuID: Int)(using PlanContext): IO[Option[(Int, Int)]] = {
    val sql =
      s"""
         |SELECT video_id, author_id
         |FROM ${schemaName}.danmaku_table
         |WHERE danmaku_id = ?;
         |""".stripMargin
    readDBJsonOptional(sql, List(SqlParameter("Int", danmakuID.toString))).map {
      _.map { json =>
        val videoID = decodeField[Int](json, "video_id")
        val senderID = decodeField[Int](json, "author_id")
        (videoID, senderID)
      }
    }
  }

  private def checkDeletionPermission(userID: Int, videoID: Int, senderID: Int, token: String)(using PlanContext): IO[Boolean] = {
    for {
      isDanmakuAuthor = userID == senderID

      isVideoUploader <- QueryVideoInfoMessage(Some(token), videoID).send.map {
        case Some(video) => video.uploaderID == userID
        case None =>
          logger.error(s"未找到视频信息，视频ID：${videoID}")
          false
      }

      isAuditor <- QueryUserRoleMessage(token).send.map {
        case Some(role) => role == UserRole.Auditor
        case None =>
          logger.error(s"无法验证用户角色，用户ID：${userID}")
          false
      }
    } yield isDanmakuAuthor || isVideoUploader || isAuditor
  }

  private def deleteDanmakuRecord(danmakuID: Int)(using PlanContext): IO[Boolean] = {
    val sql =
      s"""
         |DELETE FROM ${schemaName}.danmaku_table
         |WHERE danmaku_id = ?;
         |""".stripMargin
    writeDB(sql, List(SqlParameter("Int", danmakuID.toString))).map(_ == "Operation(s) done successfully")
  }
}