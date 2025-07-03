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
 * UpdateFeedbackLikeMessage
 * desc: 根据用户的点赞或取消点赞行为，更新相应记录。
 * @param token: String (用户身份验证令牌)
 * @param videoID: Int (唯一标识要操作的视频的ID)
 * @param isLike: Boolean (是否是点赞操作，true为点赞，false为取消点赞)
 */

case class UpdateFeedbackLikeMessage(
  token: String,
  videoID: Int,
  isLike: Boolean
) extends API[Unit](RecommendationServiceCode)

case object UpdateFeedbackLikeMessage{

  private val circeEncoder: Encoder[UpdateFeedbackLikeMessage] = deriveEncoder
  private val circeDecoder: Decoder[UpdateFeedbackLikeMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateFeedbackLikeMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateFeedbackLikeMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateFeedbackLikeMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateFeedbackLikeMessageEncoder: Encoder[UpdateFeedbackLikeMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateFeedbackLikeMessageDecoder: Decoder[UpdateFeedbackLikeMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
