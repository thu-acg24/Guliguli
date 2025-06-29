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
 * AddVideoInfoMessage
 * desc: 新增视频元数据
 * @param token: String (用户的身份验证令牌)
 * @param info: VideoInfo:1086 (视频的元数据信息，例如标题、描述、标签等)
 * @return result: String (返回操作结果，成功或包含详细错误信息)
 */

case class AddVideoInfoMessage(
  token: String,
  info: VideoInfo
) extends API[Option[String]](RecommendationServiceCode)



case object AddVideoInfoMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[AddVideoInfoMessage] = deriveEncoder
  private val circeDecoder: Decoder[AddVideoInfoMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[AddVideoInfoMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[AddVideoInfoMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[AddVideoInfoMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given AddVideoInfoMessageEncoder: Encoder[AddVideoInfoMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given AddVideoInfoMessageDecoder: Decoder[AddVideoInfoMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

