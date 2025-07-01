package Impl

import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.{minioClient, sessions}
import Objects.UserService.{UploadSession, UserInfo}
import Utils.AuthProcess.validateToken
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.http.Method
import io.minio.StatObjectArgs
import io.minio.CopyObjectArgs
import io.minio.CopySource
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.text.DecimalFormat

/**
 * Planner for ModifyUserInfoMessage: 根据用户Token校验身份后，更新用户表中newField对应字段的值，用于用户信息修改功能点
 */

case class ValidateAvatarMessagePlanner(
                                         sessionToken: String,
                                         override val planContext: PlanContext
                                       ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan definition
  override def plan(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Validating token $sessionToken"))
      session <- IO(sessions.get(sessionToken)).flatMap{
        case Some(session) if !session.completed =>
          sessions.update(sessionToken, session.copy(completed = true))
          processUploadedFile(session.objectName).as(session)

        case Some(_) =>
          IO.raiseError(new RuntimeException(s"确认上传(token:$sessionToken)已经执行过，不需要再确认"))

        case None =>
          IO.raiseError(new RuntimeException(s"不合法的sessionToken"))
      }
      _ <- updateAvatarLinkInDB(session.userID, session.objectName)
    } yield()
  }

  private def processUploadedFile(objectName: String)(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Processing uploaded file $objectName"))
      fileSize <- IO(minioClient.statObject(
        StatObjectArgs.builder()
          .bucket("temp")
          .`object`(objectName)
          .build()
      )).map(stat => stat.size())
        .handleErrorWith(ex =>
          IO(logger.info(s"Object size not found!!")) *>
            IO.raiseError(new RuntimeException(s"查找上传的文件时发生错误：${ex.getMessage}"))
        )
      _ <- IO(logger.info(s"Object size is ${getHumanReadableSize(fileSize)}"))
      _ <- if (fileSize < 10 * 1024) {
        IO.raiseError(new RuntimeException(s"上传的文件过小(${getHumanReadableSize(fileSize)}, 至少需要 10KB)"))
      } else if (fileSize > 5 * 1024 * 1024) {
        IO.raiseError(new RuntimeException(s"上传的文件过大(${getHumanReadableSize(fileSize)}, 最多只能 5MB)"))
      } else IO.unit
      _ <- IO(logger.info(s"Input is valid, moving file"))
      _ <- IO(minioClient.copyObject(
        CopyObjectArgs.builder()
          .bucket("avatar")
          .`object`(objectName)
          .source(
            CopySource.builder()
              .bucket("temp")
              .`object`(objectName)
              .build()
          ).build()
      ))
    } yield()
  }

  private def getHumanReadableSize(fileSize: Long): String = {

    val units = Array("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(fileSize.toDouble) / Math.log10(1024)).toInt
    new DecimalFormat("#,##0.#")
      .format(fileSize / Math.pow(1024, digitGroups)) + " " + units(digitGroups)
  }

  private def updateAvatarLinkInDB(userID: Int, objectName: String)(using PlanContext): IO[Unit] = {
    val querySQL =
      s"""
           UPDATE ${schemaName}.user_table
           SET avatar_path = ?
           WHERE user_id = ?
         """.stripMargin

    val queryParams = List(
      SqlParameter("String", objectName),
      SqlParameter("Int", userID.toString)
    )

    for {
      _ <- IO(logger.info(s"Executing update query: $querySQL with params: $queryParams"))
      updateResponse <- writeDB(querySQL, queryParams)
    } yield ()
  }
}