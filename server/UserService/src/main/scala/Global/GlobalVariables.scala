package Global


import Global.MinioConfig
import Global.ServiceCenter.*
import io.minio.MinioClient

object GlobalVariables {
  lazy val serviceCode : String = UserServiceCode
  val projectIDLength:Int=20
  private val minioConfig = MinioConfig.fromConfig()
  val minioClient: MinioClient = MinioClient.builder()
    .endpoint(minioConfig.endpoint)
    .credentials(minioConfig.accessKey, minioConfig.secretKey)
    .build()
  var isTest:Boolean=false

}