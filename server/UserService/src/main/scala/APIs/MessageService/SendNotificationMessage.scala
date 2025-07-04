package APIs.MessageService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.MessageServiceCode
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * SendNotificationMessage
 * desc: 根据用户 Token 验证身份后，发送通知给目标用户。
 * @param token: String (用户的身份令牌，用于验证身份。)
 * @param receiverID: Int (接收方的用户ID。)
 * @param title: String (通知标题)
 * @param messageContent: String (发送的通知的内容。)
 */

case class SendNotificationMessage(
  token: String,
  receiverID: Int,
  title: String,
  messageContent: String
) extends API[Unit](MessageServiceCode)

case object SendNotificationMessage{

  private val circeEncoder: Encoder[SendNotificationMessage] = deriveEncoder
  private val circeDecoder: Decoder[SendNotificationMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SendNotificationMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SendNotificationMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SendNotificationMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given sendNotificationMessageEncoder: Encoder[SendNotificationMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given sendNotificationMessageDecoder: Decoder[SendNotificationMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
