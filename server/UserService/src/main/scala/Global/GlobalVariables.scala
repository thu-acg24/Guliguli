package Global

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import Global.MinioConfig
import Global.ServiceCenter.*
import Objects.UploadSession
import cats.effect.{IO, Resource}
import io.minio.MinioClient
import org.http4s.client.*
import org.http4s.ember.client.*
import org.http4s.*
import Common.API.loggerFactory

import java.util.concurrent.TimeUnit

object GlobalVariables {
  lazy val serviceCode : String = UserServiceCode
  val projectIDLength:Int=20
  val minioConfig: MinioConfig = MinioConfig.fromConfig()
  val minioClient: MinioClient = {
    val tmp = try {
      MinioClient.builder()
        .endpoint(minioConfig.endpoint)
        .credentials(minioConfig.accessKey, minioConfig.secretKey)
        .build()
    } catch {
      case ex: Exception =>
        println(s"get error when building minio client: ${ex.getMessage}")
        throw new RuntimeException("Failed to create Minio client", ex)
    }
    tmp
  }
  val sessions: Cache[String, UploadSession] = Caffeine.newBuilder()
    .expireAfterWrite(30, TimeUnit.MINUTES) // 自动过期：创建或写入后30分钟
    .maximumSize(100000)                     // 限制最大缓存数，防止 OOM
    .build[String, UploadSession]()
  val clientResource: Resource[IO, Client[IO]] = EmberClientBuilder.default[IO].build
  var isTest:Boolean=false

}