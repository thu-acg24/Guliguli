package APIs.VideoService


import Common.API.API
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.VideoServiceCode
import Objects.VideoService.Video
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.parser.*
import io.circe.syntax.*
import scala.util.Try

/**
 * QueryLikeVideosMessage
 * desc: 查询用户喜欢的所有视频。
 * @param userID: Int (用户唯一标识符)
 */

case class QueryLikeVideosMessage(
  userID: Int
) extends API[List[Video]](VideoServiceCode)

case object QueryLikeVideosMessage{

  private val circeEncoder: Encoder[QueryLikeVideosMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryLikeVideosMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryLikeVideosMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryLikeVideosMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryLikeVideosMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryLikeVideosMessageEncoder: Encoder[QueryLikeVideosMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryLikeVideosMessageDecoder: Decoder[QueryLikeVideosMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
