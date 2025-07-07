package APIs.RecommendationService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.RecommendationServiceCode
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
 * RecordWatchDataMessage
 * desc: 根据用户观看视频的行为记录详细数据到观看数据表。
 * @param token: String (用户登录的身份标识Token)
 * @param videoID: Int (需要记录观看行为的视频ID)
 * @return recordable: Boolean (是否能记录进播放数)
 */

case class RecordWatchDataMessage(
  token: String,
  videoID: Int
) extends API[Boolean](RecommendationServiceCode)

case object RecordWatchDataMessage{

  private val circeEncoder: Encoder[RecordWatchDataMessage] = deriveEncoder
  private val circeDecoder: Decoder[RecordWatchDataMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[RecordWatchDataMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[RecordWatchDataMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[RecordWatchDataMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given recordWatchDataMessageEncoder: Encoder[RecordWatchDataMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given recordWatchDataMessageDecoder: Decoder[RecordWatchDataMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
