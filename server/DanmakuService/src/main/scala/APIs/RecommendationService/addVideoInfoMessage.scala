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
import Objects.RecommendationService.VideoInfo

/**
 * addVideoInfoMessage
 * desc: 新增视频元数据
 * @param token: String (用户的身份验证令牌)
 * @param info: VideoInfo:1086 (视频的元数据信息，例如标题、描述、标签等)
 * @return result: String (返回操作结果，成功或包含详细错误信息)
 */

case class addVideoInfoMessage(
  token: String,
  info: VideoInfo
) extends API[Option[String]](RecommendationServiceCode)



case object addVideoInfoMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[addVideoInfoMessage] = deriveEncoder
  private val circeDecoder: Decoder[addVideoInfoMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[addVideoInfoMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[addVideoInfoMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[addVideoInfoMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given addVideoInfoMessageEncoder: Encoder[addVideoInfoMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given addVideoInfoMessageDecoder: Decoder[addVideoInfoMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

