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
 * SendReplyNoticeMessage
 * desc: 根据用户 Token 验证身份后，发送通知给目标用户。
 * @param token: String (评论发送者的身份令牌，用于验证身份。)
 * @param commentID: Int (评论的ID。)
 */

case class SendReplyNoticeMessage(
  token: String,
  commentID: Int
) extends API[Unit](MessageServiceCode)

case object SendReplyNoticeMessage{

  private val circeEncoder: Encoder[SendReplyNoticeMessage] = deriveEncoder
  private val circeDecoder: Decoder[SendReplyNoticeMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SendReplyNoticeMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SendReplyNoticeMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SendReplyNoticeMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given sendReplyNoticeMessageEncoder: Encoder[SendReplyNoticeMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given sendReplyNoticeMessageDecoder: Decoder[SendReplyNoticeMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
