package Impl


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
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
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
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class UploadVideoMessagePlanner(
    token: String,
    videoPath: String,
    title: String,
    coverPath: String,
    description: String,
    tag: List[String],
    duration: Int,
    override val planContext: PlanContext
) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Option[String]] = {
    for {
      // Step 1: Validate token and retrieve uploaderID
      _ <- IO(logger.info("Step 1: 校验Token是否合法并获取用户ID"))
      maybeUploaderId <- validateToken()
      uploaderId <- maybeUploaderId match {
        case Some(uid) => IO.pure(uid)
        case None =>
          IO(logger.info("Token无效")) >>
            IO.pure(None) // Return None in case of invalid token
      }
      _ <- IO(logger.info(s"获取到上传者ID: $uploaderId"))

      // Step 2: Validate video information integrity
      _ <- IO(logger.info("Step 2: 校验视频信息完整性"))
      validationError <- IO(validateVideoInfo())
      _ <- validationError.fold(IO.unit)(_ => IO(logger.info(s"视频信息校验失败: ${validationError.get}")))
      if validationError.isDefined then IO.pure(Some("Invalid Video Information")) else IO.unit

      // Step 3: Check if the user has upload permissions
      _ <- IO(logger.info("Step 3: 检查用户是否有上传权限"))
      hasPermission <- checkUserPermission(uploaderId)
      if !hasPermission then IO(logger.info("用户没有权限上传视频")) >> IO.pure(Some("Permission Denied")) else IO.unit

      // Step 4: Store video information in the database
      _ <- IO(logger.info("Step 4: 添加视频到数据库"))
      writeResult <- storeVideoInfo(uploaderId)
      _ <- if (writeResult.isEmpty) IO(logger.info("视频上传成功")) else IO(logger.info(s"视频上传失败: ${writeResult.get}"))
    } yield writeResult
  }

  private def validateToken()(using PlanContext): IO[Option[Int]] = {
    getUIDByTokenMessage(token).send.map { optionalUserId =>
      IO(logger.info(s"Token验证结果: $optionalUserId"))
      optionalUserId
    }
  }

  private def validateVideoInfo(): Option[String] = {
    if (videoPath.isEmpty || title.isEmpty || coverPath.isEmpty || tag.isEmpty || duration <= 0) {
      Some("Missing or invalid video information.")
    } else {
      None
    }
  }

  private def checkUserPermission(userId: Int)(using PlanContext): IO[Boolean] = {
    for {
      userRoleOpt <- QueryUserRoleMessage(token).send
      userRole <- userRoleOpt match {
        case Some(value) => IO.pure(value)
        case None =>
          IO(logger.info("用户角色获取失败，可能Token无效或异常")) >> IO.pure(None)
      }
      result <- userRole match {
        case UserRole.Admin | UserRole.Normal => IO.pure(true)
        case UserRole.Auditor =>
          IO(logger.info("用户状态: 角色为审核员，权限受限")) >> IO.pure(false)
      }
    } yield result
  }

  private def storeVideoInfo(uploaderId: Int)(using PlanContext): IO[Option[String]] = {
    val sql =
      s"INSERT INTO ${schemaName}.video_table (title, description, duration, tag, server_path, cover_path, uploader_id, upload_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?);"
    val parameters = List(
      SqlParameter("String", title),
      SqlParameter("String", description),
      SqlParameter("Int", duration.toString),
      SqlParameter("Array[String]", tag.asJson.noSpaces),
      SqlParameter("String", videoPath),
      SqlParameter("String", coverPath),
      SqlParameter("Int", uploaderId.toString),
      SqlParameter("DateTime", DateTime.now().getMillis.toString)
    )

    writeDB(sql, parameters)
      .map(_ => None) // Success case
      .handleErrorWith { ex =>
        IO(logger.error(s"数据库存储视频信息过程发生异常: ${ex.getMessage}")) >> IO.pure(Some("Failed to upload video"))
      }
  }
}