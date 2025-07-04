package Global

import Global.ServiceCenter.*
import com.typesafe.config.{Config, ConfigFactory}
import io.minio.MinioClient

import java.io.File

case class MinioConfig(
                        endpoint: String,
                        accessKey: String,
                        secretKey: String,
                        mediaEndpoint: String
                      )

object MinioConfig {
  def fromConfig(): MinioConfig = {
    // 使用前缀避免命名冲突
    val config = load()
    // println(s"Loaded minio config: $config")

    MinioConfig(
      endpoint = config.getString("MINIO_ENDPOINT"),
      accessKey = config.getString("MINIO_ACCESS_KEY"),
      secretKey = config.getString("MINIO_SECRET_KEY"),
      mediaEndpoint = config.getString("MEDIA_ENDPOINT")
    )
  }
  private def getConfigPath(relativePath: String): String = {
    val currentDir = new File(".").getCanonicalPath
    val configFile = new File(currentDir, relativePath).getCanonicalPath

    if (!new File(configFile).exists()) {
      throw new RuntimeException(s"Config file not found: $configFile")
    }

    configFile
  }
  // 加载配置
  private def load(relativePath: String = "../minio-config.env"): Config = {
    val configPath = getConfigPath(relativePath)
    println(s"Loading minio config from: $configPath")
    ConfigFactory.parseFile(new File(configPath))
      .withFallback(ConfigFactory.load()) // 回退到 application.conf
      .resolve() // 解析变量引用
  }
}