package Objects.MessageService


import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
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
 * Message
 * desc: 消息实体，包含发送方、接收方等基本信息
 * @param messageID: Int (消息的唯一ID)
 * @param senderID: Int (发送者的用户ID)
 * @param receiverID: Int (接收者的用户ID)
 * @param content: String (消息内容)
 * @param timestamp: DateTime (消息发送的时间戳)
 * @param isNotification: Boolean (是否是一条通知消息)
 */

case class Message(
  messageID: Int,
  senderID: Int,
  receiverID: Int,
  content: String,
  timestamp: DateTime,
  isNotification: Boolean
){

  //process class code 预留标志位，不要删除

}

case object Message{

  private val circeEncoder: Encoder[Message] = deriveEncoder
  private val circeDecoder: Decoder[Message] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[Message] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[Message] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[Message]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given messageEncoder: Encoder[Message] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given messageDecoder: Decoder[Message] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
