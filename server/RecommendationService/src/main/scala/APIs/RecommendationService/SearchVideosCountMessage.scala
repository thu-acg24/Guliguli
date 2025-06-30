package APIs.RecommendationService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.RecommendationServiceCode
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
 * SearchVideosCountMessage
 * desc: 查询符合标题条件的视频总数。
 * @param searchString: String (搜索关键字，用于模糊匹配视频标题。)
 * @return searchResultCount: Int (符合条件的视频总数。)
 */

case class SearchVideosCountMessage(
  searchString: String
) extends API[Int](RecommendationServiceCode)

case object SearchVideosCountMessage{

  private val circeEncoder: Encoder[SearchVideosCountMessage] = deriveEncoder
  private val circeDecoder: Decoder[SearchVideosCountMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SearchVideosCountMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SearchVideosCountMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SearchVideosCountMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given searchVideosCountMessageEncoder: Encoder[SearchVideosCountMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given searchVideosCountMessageDecoder: Decoder[SearchVideosCountMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
