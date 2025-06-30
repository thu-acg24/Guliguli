package APIs.DanmakuService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.DanmakuServiceCode
import Objects.DanmakuService.Danmaku
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
 * QueryVideoDanmakuMessage
 * desc: 用于查询视频弹幕功能点
 * @param videoID: Int (视频的唯一标识符。)
 * @return danmaku: List[Danmaku] (与指定视频相关的弹幕记录列表。)
 */

case class QueryVideoDanmakuMessage(
  videoID: Int
) extends API[List[Danmaku]](DanmakuServiceCode)

case object QueryVideoDanmakuMessage{

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
