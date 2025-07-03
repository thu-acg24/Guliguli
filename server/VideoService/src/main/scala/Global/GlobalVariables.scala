package Global

import Common.Object.UploadSession
import scala.collection.mutable
import Global.MinioConfig
import Global.ServiceCenter.*
import io.minio.MinioClient

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
  val sessions: mutable.Map[String, UploadSession] = mutable.Map[String, UploadSession]()
  var isTest:Boolean=false

}