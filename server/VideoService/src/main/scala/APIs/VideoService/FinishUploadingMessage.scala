package APIs.VideoService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.VideoServiceCode
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * FinishUploadingMessage
 * desc: 根据用户Token校验权限后，更改视频状态
 * @param token: String (用户身份验证Token。)
 * @param videoID: Int (需要删除的视频的唯一标识ID。)
 */

case class FinishUploadingMessage(
  token: String,
  videoID: Int
) extends API[Unit](VideoServiceCode)

case object FinishUploadingMessage{

  private val circeEncoder: Encoder[FinishUploadingMessage] = deriveEncoder
  private val circeDecoder: Decoder[FinishUploadingMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[FinishUploadingMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[FinishUploadingMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[FinishUploadingMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given deleteVideoMessageEncoder: Encoder[FinishUploadingMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given deleteVideoMessageDecoder: Decoder[FinishUploadingMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
