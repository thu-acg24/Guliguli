package Common


import Global.GlobalVariables
import Global.GlobalVariables.serviceCode
import Global.MinioConfig.getConfigPath
import Global.ServiceCenter.fullNameMap
import cats.effect.IO
import com.comcast.ip4s.Port
import com.typesafe.config.{Config, ConfigFactory}
import org.http4s.Uri

import java.io.File

object ServiceUtils{
  def getURI(serviceCode: String): IO[Uri] =
    if (GlobalVariables.isTest)
      IO.fromEither(Uri.fromString(
        s"http://${serviceName(serviceCode).toLowerCase()}:" + getPort(serviceCode).value.toString + "/"
      ))
    else
      IO.fromEither(Uri.fromString(
          config.getString(serviceCode) + ":" + getPort(serviceCode).value.toString + "/"
      ))

  def getPort(serviceCode: String): Port =
    Port.fromInt(portMap(serviceCode)).getOrElse(
      throw new IllegalArgumentException(s"Invalid port for serviceCode: $serviceCode")
    )

  def serviceName(serviceCode: String): String = {
    val fullName = fullNameMap(serviceCode)
    val end = fullName.indexOf("（")
    fullNameMap(serviceCode).substring(0, end).toLowerCase
  }

  def portMap(serviceCode: String): Int = {
    serviceCode.drop(1).toInt +
      (if (serviceCode.head == 'A') 10000 else if (serviceCode.head == 'D') 20000 else 30000)
  }

  lazy val servicePort: Int = portMap(serviceCode)
  lazy val serviceFullName: String = fullNameMap(serviceCode)
  lazy val serviceShortName: String = serviceName(serviceCode)
  lazy val schemaName: String = {
    val srcSchemaName = serviceName(serviceCode).replaceAll("-","_")
    EnvUtils.isFeatBranch match {
      case true => srcSchemaName + "_" + EnvUtils.getEnv
      case false => srcSchemaName
    }
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
  private def load(relativePath: String = "../server-config.env"): Config = {
    val configPath = getConfigPath(relativePath)
    println(s"Loading server ip config from: $configPath")
    ConfigFactory.parseFile(new File(configPath))
      .withFallback(ConfigFactory.load()) // 回退到 application.conf
      .resolve() // 解析变量引用
  }

  private lazy val config = load()
}