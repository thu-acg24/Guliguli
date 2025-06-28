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


/**
 * UpdateFeedbackFavoriteMessage
 * desc: 根据用户的收藏或取消收藏行为，更新相应记录。
 * @param token: String (用户的鉴权Token，用于验证用户身份)
 * @param videoID: Int (需要操作的目标视频ID)
 * @param isFavorite: Boolean (标志是否收藏该视频，true为收藏，false为取消收藏)
 * @return result: String (操作结果。如果为空则表示成功，其他值表示失败原因)
 */

case class UpdateFeedbackFavoriteMessage(
  token: String,
  videoID: Int,
  isFavorite: Boolean
) extends API[Option[String]](RecommendationServiceCode)



case object UpdateFeedbackFavoriteMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
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

