package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.UserService.QueryUserRoleMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.APIException.InvalidInputException
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.minioClient
import Global.GlobalVariables.sessions
import Objects.UserService.UserRole
import Objects.VideoService.UploadPath
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.minio.http.Method
import java.util.concurrent.TimeUnit
import java.util.UUID
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class UploadVideoMessagePlanner(
    token: String,
    title: String,
    description: String,
    tag: List[String],
    override val planContext: PlanContext
) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate token and retrieve uploaderID
      _ <- IO(logger.info("Step 1: 校验Token是否合法并获取用户ID"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: Validate video information integrity
      _ <- IO(logger.info("Step 2: 校验视频信息完整性"))
      _ <- validateVideoInfo()

      // Step 3: Check if the user has upload permissions
      _ <- IO(logger.info("Step 3: 检查用户是否有上传权限"))
      _ <- checkUserPermission(userID)

      // Step 4: Store video information in the database
      _ <- IO(logger.info("Step 4: 添加视频到数据库"))
      _ <- storeVideoInfo(userID)
    } yield ()
  }

  private def validateVideoInfo(): IO[Unit] = {
    if (title.isEmpty) {
      IO.raiseError(InvalidInputException("输入不合法"))
    } else {
      IO.unit
    }
  }

  private def checkUserPermission(userID: Int)(using PlanContext): IO[Unit] = {
    for {
      userRole <- QueryUserRoleMessage(token).send
      result <- userRole match {
        case UserRole.Admin | UserRole.Normal => IO.unit
        case UserRole.Auditor =>
          IO(logger.info("用户状态: 角色为审核员，权限受限")) >> IO.raiseError(InvalidInputException("审核员不允许上传视频"))
      }
    } yield result
  }

  private def storeVideoInfo(userID: Int)(using PlanContext): IO[String] = {
    val sql =
      s"INSERT INTO ${schemaName}.video_table (title, description, tag, uploader_id, upload_time) VALUES (?, ?, ?, ?, ?);"
    for {
      timestamp <- IO(DateTime.now().getMillis.toString)
      parameters = List(
        SqlParameter("String", title),
        SqlParameter("String", description),
        SqlParameter("Array[String]", s"[${tag.mkString(",")}]"),
        SqlParameter("Int", userID.toString),
        SqlParameter("DateTime", timestamp)
      )
      result <- writeDB(sql, parameters)
    } yield result
  }
}