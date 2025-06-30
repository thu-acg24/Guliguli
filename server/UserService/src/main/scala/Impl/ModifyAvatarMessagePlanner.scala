package Impl


import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Global.GlobalVariables.minioClient
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
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

/**
 * Planner for ModifyUserInfoMessage: 根据用户Token校验身份后，更新用户表中newField对应字段的值，用于用户信息修改功能点
 */

case class ModifyAvatarMessagePlanner(
                                         token: String,
                                         contentType: String,
                                         override val planContext: PlanContext
                                       ) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Main plan definition
  override def plan(using PlanContext): IO[Unit] = {
    IO(logger.info(s"Validating token $token"))
    validateToken(token).flatMap {
      case Some(userID) =>
        for {
          objectName <- generateObjectName(userID)
          uploadUrl <- generateUploadUrl(objectName)
          _ <- updateAvatarLikeInDB(userID, objectName)
        } yield()
      case None =>
        IO(logger.error("Token validation failed: Invalid Token")).void
    }
  }

  private def generateObjectName(userID: Int): IO[String] = {
    for {
      timestamp <- IO.realTimeInstant.map(_.toEpochMilli)
      random <- Random.scalaUtilRandom[IO].flatMap(_.betweenInt(0, 10000))
      extension = contentType match {
        case "image/jpeg" => "jpg"
        case "image/png" => "png"
        case "image/gif" => "gif"
      }
    } yield s"$userID/$timestamp-$random.$extension"
  }

  private def generateUploadUrl(objectName: String): IO[String] = {
    IO.blocking { // 包装阻塞IO操作
      minioClient.getPresignedObjectUrl(
        io.minio.GetPresignedObjectUrlArgs.builder()
          .method(Method.PUT)
          .bucket("avatar")
          .`object`(objectName)
          .expiry(3, TimeUnit.MINUTES)
          .build()
      )
    }
  }

  private def updateAvatarLikeInDB(userID: Int, objectName: String)(using PlanContext): IO[Unit] = {
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