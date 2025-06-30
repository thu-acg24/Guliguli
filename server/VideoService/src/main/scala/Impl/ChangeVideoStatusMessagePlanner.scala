package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserRoleMessage
import APIs.VideoService.ChangeVideoStatusMessage
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
import cats.implicits._
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

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
      _ <- IO(logger.info(s"[Substep 1.1] 调用GetUIDByTokenMessage校验Token是否合法..."))
      maybeUID <- GetUIDByTokenMessage(token).send
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