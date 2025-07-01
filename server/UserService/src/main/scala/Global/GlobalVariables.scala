package Global

import scala.collection.mutable
import Global.MinioConfig
import Global.ServiceCenter.*
import Objects.UserService.UploadSession
import io.minio.MinioClient

object GlobalVariables {
  lazy val serviceCode : String = UserServiceCode
  val projectIDLength:Int=20
  private val minioConfig = MinioConfig.fromConfig()
  val minioClient: MinioClient = MinioClient.builder()
    .endpoint(minioConfig.endpoint)
    .credentials(minioConfig.accessKey, minioConfig.secretKey)
    .build()
  val sessions: mutable.Map[String, UploadSession] = mutable.Map[String, UploadSession]()
  var isTest:Boolean=false

}