package APIs.MessageService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.MessageServiceCode
import Objects.MessageService.Notification
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * QueryNotificationsMessage
 * desc: 根据用户Token验证身份后，查询当前用户和目标用户之间的私信记录。
 * @param token: String (用户的身份令牌，用于验证用户身份。)
 * @return messages: List[Notification] (私信记录的列表，包含用户之间的消息内容、时间和其他相关字段。)
 */

case class QueryNotificationsMessage(
  token: String
) extends API[List[Notification]](MessageServiceCode)

case object QueryNotificationsMessage{

  private val circeEncoder: Encoder[QueryNotificationsMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryNotificationsMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryNotificationsMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryNotificationsMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryNotificationsMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryNotificationsMessageEncoder: Encoder[QueryNotificationsMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryNotificationsMessageDecoder: Decoder[QueryNotificationsMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
