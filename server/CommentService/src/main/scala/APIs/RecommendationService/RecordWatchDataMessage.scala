package APIs.RecommendationService

import Common.API.API
import Global.ServiceCenter.RecommendationServiceCode

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.parser.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

import com.fasterxml.jackson.core.`type`.TypeReference
import Common.Serialize.JacksonSerializeUtils

import scala.util.Try

import org.joda.time.DateTime
import java.util.UUID


/**
 * RecordWatchDataMessage
 * desc: 根据用户观看视频的行为记录详细数据到观看数据表。
 * @param token: String (用户登录的身份标识Token)
 * @param videoID: Int (需要记录观看行为的视频ID)
 * @param watchDuration: Float (用户的观看时长，单位为秒)
 * @return result: String (操作结果，返回错误信息或成功标志)
 */

case class RecordWatchDataMessage(
  token: String,
  videoID: Int,
  watchDuration: Float
) extends API[Option[String]](RecommendationServiceCode)



case object RecordWatchDataMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
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

