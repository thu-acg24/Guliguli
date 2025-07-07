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

case class DeleteDanmakuMessagePlanner(
                                        token: String,
                                        danmakuID: Int,
                                        override val planContext: PlanContext
                                      ) extends Planner[Unit] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"开始验证Token并获取用户ID"))
      userID <- verifyToken(token)

      _ <- IO(logger.info(s"验证弹幕记录是否存在，弹幕ID：$danmakuID"))
      (videoID, senderID) <- checkDanmakuExistence(danmakuID)

      _ <- IO(logger.info(s"校验用户删除权限"))
      hasPermission <- checkDeletionPermission(userID, videoID, senderID, token)

      _ <- if (!hasPermission) {
        IO(logger.info(s"用户无删除权限，用户ID：$userID, 弹幕ID：$danmakuID"))
        IO.raiseError(InvalidInputException("Unauthorized Action"))
      } else IO.unit

      _ <- IO(logger.info(s"开始删除弹幕记录，弹幕ID：$danmakuID"))
      _ <- deleteDanmakuRecord(danmakuID)
    } yield ()
  }

  private def verifyToken(token: String)(using PlanContext): IO[Int] = {
    GetUIDByTokenMessage(token).send
  }

  private def checkDanmakuExistence(danmakuID: Int)(using PlanContext): IO[(Int, Int)] = {
    val sql =
      s"""
         |SELECT video_id, author_id
         |FROM $schemaName.danmaku_table
         |WHERE danmaku_id = ?;
         |""".stripMargin
    readDBJsonOptional(sql, List(SqlParameter("Int", danmakuID.toString))).map {
      case None => throw InvalidInputException("弹幕不存在")
      case Some(json) =>
        val videoID = decodeField[Int](json, "video_id")
        val senderID = decodeField[Int](json, "author_id")
        (videoID, senderID)
    }
  }

  private def checkDeletionPermission(userID: Int, videoID: Int, senderID: Int, token: String)(using PlanContext): IO[Boolean] = {
    for {
      isDanmakuAuthor <- IO(userID == senderID)

      isVideoUploader <- QueryVideoInfoMessage(Some(token), videoID).send.flatMap {
        case video => IO(video.uploaderID == userID)
        case _ =>
          IO(logger.error(s"未找到视频信息，视频ID：$videoID")) >> IO(false)
      }

      isAuditor <- QueryUserRoleMessage(token).send.flatMap {
        case role => IO(role == UserRole.Auditor)
        case _ =>
          IO(logger.error(s"无法验证用户角色，用户ID：$userID")) >> IO(false)
      }
    } yield isDanmakuAuthor || isVideoUploader || isAuditor
  }

  private def deleteDanmakuRecord(danmakuID: Int)(using PlanContext): IO[String] = {
    val sql =
      s"""
         |DELETE FROM $schemaName.danmaku_table
         |WHERE danmaku_id = ?;
         |""".stripMargin
    writeDB(sql, List(SqlParameter("Int", danmakuID.toString)))
  }
}