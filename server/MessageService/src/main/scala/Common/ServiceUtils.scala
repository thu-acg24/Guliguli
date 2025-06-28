package Common

import Global.GlobalVariables.serviceCode
import Global.GlobalVariables
import Global.ServiceCenter.fullNameMap
import cats.effect.IO
import com.comcast.ip4s.Port
import org.http4s.Uri


object ServiceUtils{
  def getURI(serviceCode: String): IO[Uri] =
    if (GlobalVariables.isTest)
      IO.fromEither(Uri.fromString(
        s"http://${serviceName(serviceCode).toLowerCase()}:" + getPort(serviceCode).value.toString + "/"
      ))
    else
      IO.fromEither(Uri.fromString(
        "http://localhost:" + getPort(serviceCode).value.toString + "/"
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
}
