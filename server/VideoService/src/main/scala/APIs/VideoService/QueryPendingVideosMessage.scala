package APIs.VideoService

import Common.API.API
import Global.ServiceCenter.VideoServiceCode

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
import Objects.VideoService.Video

/**
 * QueryPendingVideosMessage
 * desc: 获取所有待审核的视频信息
 * @param token: String (用于验证身份的令牌)
 * @return pendingVideos: Video:1120 (封装所有待审核视频的列表)
 */

case class QueryPendingVideosMessage(
  token: String
) extends API[List[Video]](VideoServiceCode)



case object QueryPendingVideosMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryPendingVideosMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryPendingVideosMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryPendingVideosMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryPendingVideosMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryPendingVideosMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryPendingVideosMessageEncoder: Encoder[QueryPendingVideosMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryPendingVideosMessageDecoder: Decoder[QueryPendingVideosMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

