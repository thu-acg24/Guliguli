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
 * SearchVideosMessage
 * desc: 提供模糊搜索功能，返回匹配的视频列表。
 * @param token: Option[String] (用户Token，可选)
 * @param searchString: String (搜索关键字，用于匹配视频标题。)
 * @param fetchLimit: Int (返回结果数上限)
 * @return searchResult: List[Video] (返回的视频列表，包含匹配的所有视频信息。)
 */

case class SearchVideosMessage(
  token: Option[String],
  searchString: String,
  fetchLimit: Int = 20,
) extends API[List[Video]](RecommendationServiceCode)

case object SearchVideosMessage{

  private val circeEncoder: Encoder[SearchVideosMessage] = deriveEncoder
  private val circeDecoder: Decoder[SearchVideosMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SearchVideosMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SearchVideosMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SearchVideosMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given searchVideosMessageEncoder: Encoder[SearchVideosMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given searchVideosMessageDecoder: Decoder[SearchVideosMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
