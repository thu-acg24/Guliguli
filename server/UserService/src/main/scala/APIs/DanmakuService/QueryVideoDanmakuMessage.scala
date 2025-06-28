package APIs.DanmakuService

import Common.API.API
import Global.ServiceCenter.DanmakuServiceCode

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
import Objects.DanmakuService.Danmaku

/**
 * QueryVideoDanmakuMessage
 * desc: 用于查询视频弹幕功能点
 * @param videoID: Int (视频的唯一标识符。)
 * @return danmakus: Danmaku:1042 (与指定视频相关的弹幕记录列表。)
 */

case class QueryVideoDanmakuMessage(
  videoID: Int
) extends API[Option[List[Danmaku]]](DanmakuServiceCode)



case object QueryVideoDanmakuMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryVideoDanmakuMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryVideoDanmakuMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryVideoDanmakuMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryVideoDanmakuMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryVideoDanmakuMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryVideoDanmakuMessageEncoder: Encoder[QueryVideoDanmakuMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryVideoDanmakuMessageDecoder: Decoder[QueryVideoDanmakuMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

