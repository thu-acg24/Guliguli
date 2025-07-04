package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.minioClient
import Global.GlobalVariables.sessions
import Objects.UploadSession
import Objects.UserService.UserInfo
import Utils.AuthProcess.validateToken
import cats.effect.IO
import cats.effect.std.Random
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import io.minio.http.Method
import java.util.concurrent.TimeUnit
import java.util.UUID
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * Planner for ModifyUserInfoMessage: 根据用户Token校验身份后，更新用户表中newField对应字段的值，用于用户信息修改功能点
 */

case class ModifyAvatarMessagePlanner(
                                         token: String,
                                         override val planContext: PlanContext
                                       ) extends Planner[List[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan definition
  override def plan(using PlanContext): IO[List[String]] = {
    for {
      _ <- IO(logger.info(s"Validating token $token"))
      userID <- validateToken(token)
      objectName <- generateObjectName(userID)
      uploadUrl <- generateUploadUrl(objectName)
      sessionToken <- IO(UUID.randomUUID().toString)
      _ <- IO(sessions.put(sessionToken, UploadSession(sessionToken, userID, objectName)))
    } yield List(uploadUrl, sessionToken)
  }

  private def generateObjectName(userID: Int): IO[String] = {
    for {
      timestamp <- IO.realTimeInstant.map(_.toEpochMilli)
      random <- Random.scalaUtilRandom[IO].flatMap(_.betweenInt(0, 10000))
    } yield s"$userID/$timestamp-$random.jpg"
  }

  private def generateUploadUrl(objectName: String): IO[String] = {
    IO.blocking { // 包装阻塞IO操作
      minioClient.getPresignedObjectUrl(
        io.minio.GetPresignedObjectUrlArgs.builder()
          .method(Method.PUT)
          .bucket("temp")
          .`object`(objectName)
          .expiry(3, TimeUnit.MINUTES)
          .build()
      )
    }
  }
}