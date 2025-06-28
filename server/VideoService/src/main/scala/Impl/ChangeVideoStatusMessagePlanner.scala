package Impl


import APIs.VideoService.ChangeVideoStatusMessage
import Objects.VideoService.VideoStatus
import Objects.VideoService.Video
import Objects.UserService.UserRole
import APIs.UserService.QueryUserRoleMessage
import APIs.UserService.getUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import cats.implicits._
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
import APIs.UserService.getUIDByTokenMessage
import APIs.UserService.{QueryUserRoleMessage, getUIDByTokenMessage}
import Objects.VideoService.{Video, VideoStatus}
import io.circe._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ChangeVideoStatusMessagePlanner(
    token: String,
    videoID: Int,
    status: VideoStatus,
    override val planContext: PlanContext
) extends Planner[Option[String]] {
  val logger =
    LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: 校验Token和权限
      _ <- IO(logger.info("[Step 1] 开始校验Token和权限..."))
      isAuthorized <- validateTokenAndPermissions
      _ <- IO(
        logger.info(
          s"[Step 1] Token校验和权限结果: ${
            isAuthorized.fold("未授权用户")(_ => "授权通过")
          }"
        )
      )
      result <- isAuthorized match {
        case None => IO.pure(None) // 未通过校验，直接返回None
        case Some(_) =>
          for {
            // Step 2: 校验视频ID
            _ <- IO(logger.info(s"[Step 2] 开始校验视频ID: $videoID..."))
            isValidVideo <- validateVideoID
            _ <- IO(
              logger.info(
                s"[Step 2] 视频ID校验结果: ${if (isValidVideo) "有效" else "无效"}"
              )
            )
            result <- if (!isValidVideo) IO.pure(None)
            else
              for {
                // Step 3: 修改视频状态
                _ <- IO(logger.info(s"[Step 3] 开始修改视频状态为: $status..."))
                updateResult <- ChangeVideoStatusMessage(
                  token,
                  videoID,
                  status
                ).send
                _ <- IO(
                  logger.info(
                    s"[Step 3] 修改视频状态结果: ${
                      updateResult.getOrElse("操作失败: 返回值为None")
                    }"
                  )
                )
              } yield updateResult
          } yield result
      }
    } yield result
  }

  // 校验用户的Token和权限
  private def validateTokenAndPermissions()(using PlanContext): IO[Option[Int]] = {
    for {
      _ <- IO(logger.info(s"[Substep 1.1] 调用getUIDByTokenMessage校验Token是否合法..."))
      maybeUID <- getUIDByTokenMessage(token).send
      _ <- IO(logger.info(s"[Substep 1.1] 解析到的用户ID为: ${maybeUID.getOrElse("无效的Token")}"))
      userRole <- maybeUID match {
        case None => IO.pure(None) // Token无效
        case Some(_) =>
          for {
            _ <- IO(logger.info(s"[Substep 1.2] 调用QueryUserRoleMessage校验用户审核员权限..."))
            roleOption <- QueryUserRoleMessage(token).send
            _ <- IO(
              logger.info(
                s"[Substep 1.2] 获得用户角色: ${roleOption.map(_.toString).getOrElse("None")}"
              )
            )
          } yield
            roleOption match {
              case Some(UserRole.Auditor) => maybeUID // 只有Auditor角色才有权限
              case _ => None
            }
      }
    } yield userRole
  }

  // 校验视频ID是否有效且视频未被删除
  private def validateVideoID()(using PlanContext): IO[Boolean] = {
    val query =
      s"SELECT 1 FROM ${schemaName}.video WHERE video_id = ? AND status <> 'Deleted';"
    val params = List(SqlParameter("Int", videoID.toString))
    readDBJsonOptional(query, params).map(_.isDefined)
  }
}