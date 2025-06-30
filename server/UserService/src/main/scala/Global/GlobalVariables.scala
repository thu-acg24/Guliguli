package Global

import Global.ServiceCenter.*
import io.minio.MinioClient
import Global.MinioConfig


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
