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
 * UpdateFeedbackFavoriteMessage
 * desc: 根据用户的收藏或取消收藏行为，更新相应记录。
 * @param token: String (用户的鉴权Token，用于验证用户身份)
 * @param videoID: Int (需要操作的目标视频ID)
 * @param isFavorite: Boolean (标志是否收藏该视频，true为收藏，false为取消收藏)
 */

case class UpdateFeedbackFavoriteMessage(
  token: String,
  videoID: Int,
  isFavorite: Boolean
) extends API[Unit](RecommendationServiceCode)

case object UpdateFeedbackFavoriteMessage{

  private val circeEncoder: Encoder[UpdateFeedbackFavoriteMessage] = deriveEncoder
  private val circeDecoder: Decoder[UpdateFeedbackFavoriteMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateFeedbackFavoriteMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateFeedbackFavoriteMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateFeedbackFavoriteMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateFeedbackFavoriteMessageEncoder: Encoder[UpdateFeedbackFavoriteMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateFeedbackFavoriteMessageDecoder: Decoder[UpdateFeedbackFavoriteMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
