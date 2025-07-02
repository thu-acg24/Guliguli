package Objects.MessageService

import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * NoticesCount
 * desc: 消息实体，包含发送方、接收方等基本信息
 * @param MessagesCount: Int (私信的数量)
 * @param NotificationsCount: Int (通知的数量)
 * @param ReplyNoticesCount: Int (回复的数量)
 */

case class NoticesCount(
  MessagesCount: Int,
  NotificationsCount: Int,
  ReplyNoticesCount: Int
){

  //process class code 预留标志位，不要删除

}

case object NoticesCount{

  private val circeEncoder: Encoder[NoticesCount] = deriveEncoder
  private val circeDecoder: Decoder[NoticesCount] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[NoticesCount] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[NoticesCount] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[NoticesCount]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given messageEncoder: Encoder[NoticesCount] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given messageDecoder: Decoder[NoticesCount] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
