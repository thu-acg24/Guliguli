package Global

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import Global.ServiceCenter.*
import cats.effect.{IO, Resource}
import io.minio.{GetObjectArgs, MinioClient}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import Common.API.loggerFactory
import Objects.UploadSession

import java.io.InputStream
import java.util.concurrent.TimeUnit
import fs2.io.readInputStream
import fs2.text

object GlobalVariables {
  lazy val serviceCode : String = VideoServiceCode
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

  // 获取 MinIO 中对象的 InputStream
  def getObjectStream(bucket: String, key: String): Resource[IO, InputStream] =
    Resource.make {
      IO.blocking(minioClient.getObject(
        GetObjectArgs.builder()
          .bucket(bucket)
          .`object`(key)
          .build()
      ))
    }(in => IO.blocking(in.close()))

  // 读取 InputStream 为字符串列表
  def readLines(bucket: String, key: String): IO[List[String]] =
    getObjectStream(bucket, key).use { in =>
      readInputStream(IO.pure(in), 8192, closeAfterUse = false)
        .through(text.utf8.decode)
        .through(text.lines)
        .compile
        .toList
    }


  val sessions: Cache[String, UploadSession] = Caffeine.newBuilder()
    .expireAfterWrite(30, TimeUnit.MINUTES) // 自动过期：创建或写入后30分钟
    .maximumSize(100000)                     // 限制最大缓存数，防止 OOM
    .build[String, UploadSession]()

  val m3u8Cache: Cache[String, String] = Caffeine.newBuilder()
    .expireAfterWrite(30, TimeUnit.DAYS) // 自动过期：创建或写入后30分钟
    .maximumSize(100000)
    .build[String, String]()

  val clientResource: Resource[IO, Client[IO]] = EmberClientBuilder.default[IO].build
  var isTest:Boolean=false

}