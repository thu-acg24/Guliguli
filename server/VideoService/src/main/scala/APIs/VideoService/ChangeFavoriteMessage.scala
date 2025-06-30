package APIs.VideoService

import Common.API.API
import Global.ServiceCenter.VideoServiceCode

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
 * ChangeFavoriteMessage
 * desc: 增或删用户收藏记录。
 * @param token: String (用户身份认证Token)
 * @param videoID: Int (视频唯一标识符)
 * @param isFav: Boolean (表示是否收藏(true表示新增收藏，false表示取消收藏))
 */

case class ChangeFavoriteMessage(
  token: String,
  videoID: Int,
  isFav: Boolean
) extends API[Unit](VideoServiceCode)



case object ChangeFavoriteMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ChangeFavoriteMessage] = deriveEncoder
  private val circeDecoder: Decoder[ChangeFavoriteMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ChangeFavoriteMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ChangeFavoriteMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ChangeFavoriteMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given changeFavoriteMessageEncoder: Encoder[ChangeFavoriteMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given changeFavoriteMessageDecoder: Decoder[ChangeFavoriteMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

