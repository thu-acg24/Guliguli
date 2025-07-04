package Global

import Common.Object.UploadSession

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import Global.ServiceCenter.*
import io.minio.MinioClient

import java.util.concurrent.TimeUnit

object GlobalVariables {
  lazy val serviceCode : String = VideoServiceCode
  val projectIDLength:Int=20
  private val minioConfig = MinioConfig.fromConfig()
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
  var isTest:Boolean=false

}