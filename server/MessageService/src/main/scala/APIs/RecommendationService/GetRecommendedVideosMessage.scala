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
import Objects.VideoService.Video

/**
 * GetRecommendedVideosMessage
 * desc: 基于用户行为或视频标签生成推荐视频列表
 * @param videoID: Int (视频ID，用于基于视频相关性生成推荐。)
 * @param userID: Int (用户ID，用于基于用户行为生成推荐。)
 * @return recommendedVideos: Video:1120 (推荐的视频列表，包含视频的完整信息。)
 */

case class GetRecommendedVideosMessage(
  videoID: Option[Int] = None,
  userID: Option[Int] = None
) extends API[List[Video]](RecommendationServiceCode)



case object GetRecommendedVideosMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[GetRecommendedVideosMessage] = deriveEncoder
  private val circeDecoder: Decoder[GetRecommendedVideosMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[GetRecommendedVideosMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[GetRecommendedVideosMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[GetRecommendedVideosMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given getRecommendedVideosMessageEncoder: Encoder[GetRecommendedVideosMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given getRecommendedVideosMessageDecoder: Decoder[GetRecommendedVideosMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

