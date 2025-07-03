package APIs.VideoService


import Common.API.API
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.VideoServiceCode
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
 * QueryLikeMessage
 * desc: 查询用户是否喜欢某个视频。
 * @param token: String (用户身份认证Token)
 * @param videoID: Int (视频唯一标识符)
 */

case class QueryLikeMessage(
  token: String,
  videoID: Int
) extends API[Boolean](VideoServiceCode)

case object QueryLikeMessage{

  private val circeEncoder: Encoder[QueryLikeMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryLikeMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryLikeMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryLikeMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryLikeMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryLikeMessageEncoder: Encoder[QueryLikeMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryLikeMessageDecoder: Decoder[QueryLikeMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
