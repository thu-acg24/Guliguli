package Global


import Common.Serialize.JacksonSerializeUtils
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import io.circe.generic.auto.*
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import scala.util.Try

/** 配置文件 */
case class ServerConfig(
                         /** 服务器地址 */
                         serverIP: String,

                         /** 服务器端口 */
                         serverPort: Int = Common.ServiceUtils.servicePort,

                         /** 最大连接数 */
                         maximumServerConnection: Int,

                         /** 最大的同时往内部微服务发送的请求个数，原则上和最大连接数相同 */
                         maximumClientConnection: Int,

                         isTest:Boolean
                       )

case object ServerConfig{

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ServerConfig] = deriveEncoder
  private val circeDecoder: Decoder[ServerConfig] = deriveDecoder

  // jackson 对应的 encoder, decoder
  private val jacksonEncoder: Encoder[ServerConfig] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ServerConfig] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ServerConfig]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history))}
  }

  // Circe + Jackson 兜底的 Encoder
  given dbConfigEncoder: Encoder[ServerConfig] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given dbConfigDecoder: Decoder[ServerConfig] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}