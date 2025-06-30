package APIs.MessageService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.MessageServiceCode
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
 * SendMessageMessage
 * desc: 根据用户 Token 验证身份后，发送私信或通知给目标用户。
 * @param token: String (用户的身份令牌，用于验证身份。)
 * @param receiverID: Int (接收方的用户ID。)
 * @param messageContent: String (发送的私信或通知的内容。)
 * @param isNotification: Boolean (此消息是否为通知类型。)
 */

case class SendMessageMessage(
  token: String,
  receiverID: Int,
  messageContent: String,
  isNotification: Boolean
) extends API[Unit](MessageServiceCode)

case object SendMessageMessage{

  private val circeEncoder: Encoder[SendMessageMessage] = deriveEncoder
  private val circeDecoder: Decoder[SendMessageMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SendMessageMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SendMessageMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SendMessageMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given sendMessageMessageEncoder: Encoder[SendMessageMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given sendMessageMessageDecoder: Decoder[SendMessageMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
