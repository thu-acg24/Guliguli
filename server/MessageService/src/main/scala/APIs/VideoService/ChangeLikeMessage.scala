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
 * ChangeLikeMessage
 * desc: 增或删用户点赞记录。
 * @param token: String (用户的校验Token，用于确认登录身份)
 * @param videoID: Int (视频的唯一标识符)
 * @param isLike: Boolean (点赞操作标志，true表示点赞，false表示取消点赞)
 * @return result: String (返回操作的结果信息（None表示成功，否则为错误信息）)
 */

case class ChangeLikeMessage(
  token: String,
  videoID: Int,
  isLike: Boolean
) extends API[Option[String]](VideoServiceCode)



case object ChangeLikeMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ChangeLikeMessage] = deriveEncoder
  private val circeDecoder: Decoder[ChangeLikeMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ChangeLikeMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ChangeLikeMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ChangeLikeMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given changeLikeMessageEncoder: Encoder[ChangeLikeMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given changeLikeMessageDecoder: Decoder[ChangeLikeMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

