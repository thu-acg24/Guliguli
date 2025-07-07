package APIs.VideoService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.VideoServiceCode
import Objects.VideoService.UploadPath
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * QueryM3U8PathMessage
 * desc: 根据用户Token校验身份后，获取视频播放链接
 * @param token: String (用户身份校验的Token)
 * @param videoID: Int (视频ID)
 * @return M3U8Path: String (播放链接)
 */

case class QueryM3U8PathMessage(
  token: Option[String],
  videoID: Int,
) extends API[String](VideoServiceCode)

case object QueryM3U8PathMessage {

  private val circeEncoder: Encoder[QueryM3U8PathMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryM3U8PathMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryM3U8PathMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryM3U8PathMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces,
          new TypeReference[QueryM3U8PathMessage]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given QueryM3U8PathMessageEncoder: Encoder[QueryM3U8PathMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given QueryM3U8PathMessageDecoder: Decoder[QueryM3U8PathMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
