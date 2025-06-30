package Global

import Global.ServiceCenter.*
import io.minio.MinioClient
import com.typesafe.config.{Config, ConfigFactory}
import java.io.File

case class MinioConfig(
                        endpoint: String,
                        accessKey: String,
                        secretKey: String
                      )

object MinioConfig {
  def fromConfig(): MinioConfig = {
    // 使用前缀避免命名冲突
    val config = load()

    MinioConfig(
      endpoint = config.getString("endpoint"),
      accessKey = config.getString("accessKey"),
      secretKey = config.getString("secretKey")
    )
  }
  private def getConfigPath(relativePath: String): String = {
    val currentDir = new File(".").getCanonicalPath
    val configFile = new File(currentDir, relativePath).getCanonicalPath
    println(s"Loading config from: $configFile")

    if (!new File(configFile).exists()) {
      throw new RuntimeException(s"Config file not found: $configFile")
    }

    configFile
  }
  // 加载配置
  private def load(relativePath: String = "../minio-config.env"): Config = {
    val configPath = getConfigPath(relativePath)

    ConfigFactory.parseFile(new File(configPath))
      .withFallback(ConfigFactory.load()) // 回退到 application.conf
      .resolve() // 解析变量引用
  }
}