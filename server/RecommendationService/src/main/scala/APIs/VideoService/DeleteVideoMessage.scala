package APIs.VideoService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
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
import java.util.UUID
import org.joda.time.DateTime
import scala.util.Try

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
