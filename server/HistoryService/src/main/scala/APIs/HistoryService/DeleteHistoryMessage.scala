package APIs.HistoryService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.HistoryServiceCode
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
 * DeleteHistoryMessage
 * desc: 根据用户Token校验后，从历史记录表删除指定记录。
 * @param token: String (用户的身份令牌，用于校验用户是否合法。)
 * @param videoID: Int (需要删除的历史记录中对应的视频ID。)
 */

case class DeleteHistoryMessage(
  token: String,
  videoID: Int
) extends API[Unit](HistoryServiceCode)

case object DeleteHistoryMessage{

  private val circeEncoder: Encoder[DeleteHistoryMessage] = deriveEncoder
  private val circeDecoder: Decoder[DeleteHistoryMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[DeleteHistoryMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[DeleteHistoryMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[DeleteHistoryMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given deleteHistoryMessageEncoder: Encoder[DeleteHistoryMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given deleteHistoryMessageDecoder: Decoder[DeleteHistoryMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
