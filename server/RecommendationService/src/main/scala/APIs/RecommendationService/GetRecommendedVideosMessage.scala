package APIs.RecommendationService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.RecommendationServiceCode
import Objects.VideoService.Video
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
 * GetRecommendedVideosMessage
 * desc: 基于用户行为或视频标签生成推荐视频列表
 * @param videoID: Int (视频ID，用于基于视频相关性生成推荐。)
 * @param userID: Int (用户ID，用于基于用户行为生成推荐。)
 * @return recommendedVideos: Video:1120 (推荐的视频列表，包含视频的完整信息。)
 */

case class GetRecommendedVideosMessage(
  videoID: Option[Int] = None,
  userID: Option[String] = None,
  randomRatio: Float = 0.2F
) extends API[List[Video]](RecommendationServiceCode)

case object GetRecommendedVideosMessage{

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
