package Impl


/**
 * Planner for DeleteVideoMessage: 根据用户Token校验权限后，根据videoID删除视频记录
 */
import Objects.UserService.UserRole
import APIs.UserService.QueryUserRoleMessage
import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe.Json
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
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
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class DeleteVideoMessagePlanner(
    token: String,
    videoID: Int,
    override val planContext: PlanContext
) extends Planner[Option[String]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = for {
    _ <- IO(logger.info(s"开始执行DeleteVideoMessagePlanner, token=${token}, videoID=${videoID}"))

    // Step 1: Validate token and fetch user ID
    maybeUserID <- validateToken(token)
    _ <- IO(logger.info(s"获取到用户ID: ${maybeUserID.getOrElse("None")}"))
    _ <- IO.raiseUnless(maybeUserID.isDefined)(new IllegalArgumentException("Invalid Token"))
    userID = maybeUserID.get

    // Step 2: Validate video existence and fetch uploader ID
    maybeUploaderID <- fetchUploaderID(videoID)
    _ <- IO(logger.info(s"获取到视频的上传者ID: ${maybeUploaderID.getOrElse("None")}"))
    _ <- IO.raiseUnless(maybeUploaderID.isDefined)(new IllegalStateException("Video Not Found"))
    uploaderID = maybeUploaderID.get

    // Step 3: Check user permissions
    hasPermission <- checkPermissions(userID, uploaderID)
    _ <- IO(logger.info(s"权限校验结果: ${hasPermission}"))
    _ <- IO.raiseUnless(hasPermission)(new IllegalAccessException("Permission Denied"))

    // Step 4: Delete the video
    deletionResult <- deleteVideo(videoID)
    _ <- IO(logger.info(s"视频删除结果: ${deletionResult}"))

  } yield if (deletionResult) None else Some("Failed to delete video")

  /**
   * 校验Token合法性，并获取用户ID
   */
  private def validateToken(token: String)(using PlanContext): IO[Option[Int]] = {
    logger.info("开始校验Token的合法性")
    GetUIDByTokenMessage(token).send
  }

  /**
   * 校验视频ID是否存在，并获取上传者ID
   */
  private def fetchUploaderID(videoID: Int)(using PlanContext): IO[Option[Int]] = {
    logger.info(s"开始校验视频ID是否合法, videoID=${videoID}")
    val sql =
      s"""
        SELECT uploader_id
        FROM ${schemaName}.video_table
        WHERE video_id = ?;
      """
    readDBJsonOptional(sql, List(SqlParameter("Int", videoID.toString)))
      .map(_.map(json => decodeField[Int](json, "uploader_id")))
  }

  /**
   * 检查用户是否有权限删除视频
   */
  private def checkPermissions(
      userID: Int,
      uploaderID: Int
  )(using PlanContext): IO[Boolean] = {
    logger.info(s"开始校验用户是否有权限删除该视频, userID=${userID}, uploaderID=${uploaderID}")
    if (userID == uploaderID) {
      IO(true) // 上传者本人有权限
    } else {
      QueryUserRoleMessage(token).send.map {
        case Some(role) if role == UserRole.Auditor => true // 审核员有权限
        case _                                     => false
      }
    }
  }

  /**
   * 执行视频删除操作
   */
  private def deleteVideo(videoID: Int)(using PlanContext): IO[Boolean] = {
    logger.info(s"开始删除视频记录, videoID=${videoID}")
    val sql =
      s"""
        DELETE FROM ${schemaName}.video_table
        WHERE video_id = ?;
      """
    writeDB(sql, List(SqlParameter("Int", videoID.toString)))
      .map(_ => true)
      .handleErrorWith { err =>
        logger.error(s"删除视频时发生错误: ${err.getMessage}")
        IO(false)
      }
  }
}