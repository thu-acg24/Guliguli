package APIs.MessageService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.MessageServiceCode
import Objects.MessageService.Message
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
 * QueryMessagesMessage
 * desc: 根据用户Token验证身份后，查询当前用户和目标用户之间的私信记录。
 * @param token: String (用户的身份令牌，用于验证用户身份。)
 * @param targetID: Int (目标用户的用户ID，用于查询与该用户的私信记录。)
 * @return messages: List[Message] (私信记录的列表，包含用户之间的消息内容、时间和其他相关字段。)
 */

case class QueryMessagesMessage(
  token: String,
  targetID: Int
) extends API[List[Message]](MessageServiceCode)

case object QueryMessagesMessage{

  private val circeEncoder: Encoder[QueryMessagesMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryMessagesMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryMessagesMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryMessagesMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryMessagesMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryMessagesMessageEncoder: Encoder[QueryMessagesMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryMessagesMessageDecoder: Decoder[QueryMessagesMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
