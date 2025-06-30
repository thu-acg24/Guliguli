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
 * DeleteVideoMessage
 * desc: 根据用户Token校验权限后，根据videoID删除视频记录。
 * @param token: String (用户身份验证Token。)
 * @param videoID: Int (需要删除的视频的唯一标识ID。)
 */

case class DeleteVideoMessage(
  token: String,
  videoID: Int
) extends API[Unit](VideoServiceCode)



case object DeleteVideoMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[DeleteVideoMessage] = deriveEncoder
  private val circeDecoder: Decoder[DeleteVideoMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[DeleteVideoMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[DeleteVideoMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[DeleteVideoMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given deleteVideoMessageEncoder: Encoder[DeleteVideoMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given deleteVideoMessageDecoder: Decoder[DeleteVideoMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

