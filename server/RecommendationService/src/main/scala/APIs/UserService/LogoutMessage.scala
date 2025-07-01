package APIs.UserService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.UserServiceCode
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.parser.*
import io.circe.syntax.*
import java.util.UUID
import org.joda.time.DateTime
import scala.util.Try

/**
 * LogoutMessage
 * desc: 根据用户Token，移除当前用户登录状态，并销毁存储的Token。用于用户登出功能点。
 * @param token: String (用户登录Token，用于校验用户身份)
 */

case class LogoutMessage(
  token: String
) extends API[Unit](UserServiceCode)

case object LogoutMessage{

  private val circeEncoder: Encoder[LogoutMessage] = deriveEncoder
  private val circeDecoder: Decoder[LogoutMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[LogoutMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[LogoutMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[LogoutMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given logoutMessageEncoder: Encoder[LogoutMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given logoutMessageDecoder: Decoder[LogoutMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
