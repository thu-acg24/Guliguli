package Process

import Common.API.{PlanContext, TraceID}
import Global.{DBConfig, ServerConfig, ServerConfigDecoder}
import cats.effect.{IO, Resource}
import io.circe.*
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.Request
import org.http4s.circe.CirceEntityCodec.*

import java.util.UUID
import scala.io.{BufferedSource, Source}

object ConfigUtils {
  /** 读取config文件 */
  def readConfig(filePath: String): IO[ServerConfig] = {
    // Define a resource for managing the file
    val fileResource: Resource[IO, BufferedSource] = Resource.make {
      IO(Source.fromFile(filePath)) // Acquire the resource
    } { source =>
      IO(source.close()).handleErrorWith(e => IO{e.printStackTrace()}) // Release the resource, ignoring errors on close
    }
    

    // Use the resource
    fileResource.use { source =>
      IO {
        val fileContents = source.getLines().mkString
        ServerConfigDecoder.parser(fileContents) match {
          case Right(config) => {
            config
          }
          case Left(error) => throw new RuntimeException(s"Failed to decode config: $error")
        }
      }
    }
  }
  def handlePostRequest(req: Request[IO]): IO[String] = {
    req.as[Json].map { bodyJson =>
      val hasPlanContext = bodyJson.hcursor.downField("planContext").succeeded

      val updatedJson = if (hasPlanContext) {
        bodyJson
      } else {
        val planContext = PlanContext(TraceID(UUID.randomUUID().toString), transactionLevel = 0)
        val planContextJson = planContext.asJson
        bodyJson.deepMerge(Json.obj("planContext" -> planContextJson))
      }
      updatedJson.toString
    }
  }

  def server2DB(serviceConfig: ServerConfig): DBConfig = {
    DBConfig(
      jdbcUrl = serviceConfig.jdbcUrl, 
      username = serviceConfig.username, 
      password = serviceConfig.password, 
      schemaName = Common.ServiceUtils.schemaName, 
      prepStmtCacheSize = serviceConfig.prepStmtCacheSize, 
      prepStmtCacheSqlLimit = serviceConfig.prepStmtCacheSqlLimit, 
      maximumPoolSize = serviceConfig.maximumPoolSize, 
      connectionLiveMinutes = serviceConfig.connectionLiveMinutes, 
      maximumServerConnection = serviceConfig.maximumServerConnection
    )
  }

}

