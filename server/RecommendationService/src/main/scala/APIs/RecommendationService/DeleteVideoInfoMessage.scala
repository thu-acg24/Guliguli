package APIs.RecommendationService

import Common.API.API
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.RecommendationServiceCode
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}

import scala.util.Try


/**
 * DeleteVideoInfoMessage
 * desc: 删除视频元数据
 * @param token: String (用户验证Token，用于验证当前用户的身份权限)
 * @param videoID: Int (视频的唯一标识符，表示需要删除的视频)
 */

case class DeleteVideoInfoMessage(
  token: String,
  videoID: Int
) extends API[Unit](RecommendationServiceCode)



case object DeleteVideoInfoMessage{

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[DeleteVideoInfoMessage] = deriveEncoder
  private val circeDecoder: Decoder[DeleteVideoInfoMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[DeleteVideoInfoMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[DeleteVideoInfoMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[DeleteVideoInfoMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given DeleteVideoInfoMessageEncoder: Encoder[DeleteVideoInfoMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given DeleteVideoInfoMessageDecoder: Decoder[DeleteVideoInfoMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

