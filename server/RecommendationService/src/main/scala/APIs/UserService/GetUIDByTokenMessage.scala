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
 * GetUIDByTokenMessage
 * desc: 根据用户Token，返回该Token对应的用户ID，Token不合法则返回None。
 * @param token: String (用户的会话Token，用于校验身份。)
 * @return userID: Int (Token解析后的用户ID)
 */

case class GetUIDByTokenMessage(
  token: String
) extends API[Int](UserServiceCode)

case object GetUIDByTokenMessage{

  private val circeEncoder: Encoder[GetUIDByTokenMessage] = deriveEncoder
  private val circeDecoder: Decoder[GetUIDByTokenMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[GetUIDByTokenMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[GetUIDByTokenMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[GetUIDByTokenMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given GetUIDByTokenMessageEncoder: Encoder[GetUIDByTokenMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given GetUIDByTokenMessageDecoder: Decoder[GetUIDByTokenMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
