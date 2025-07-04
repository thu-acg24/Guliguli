package APIs.VideoService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.VideoServiceCode
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * ConfirmVideoMessage
 * desc: 通知服务器视频已处理完毕
 * @param sessionToken: String (会话Token)
 * @param status: String (处理状态)
 * @param m3u8Name: String (m3u8文件名)
 * @param tsPrefix: String (ts分片前缀)
 * @param sliceCount: String (ts分片数)
 * @param duration: Float (时长)
 */

case class ConfirmVideoMessage(
  sessionToken: String,
  status: String,
  m3u8Name: String,
  tsPrefix: String,
  sliceCount: Int,
  duration: Float,
) extends API[Unit](VideoServiceCode)

case object ConfirmVideoMessage{

  private val circeEncoder: Encoder[ConfirmVideoMessage] = deriveEncoder
  private val circeDecoder: Decoder[ConfirmVideoMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ConfirmVideoMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ConfirmVideoMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ConfirmVideoMessage]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given confirmVideoMessageEncoder: Encoder[ConfirmVideoMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given confirmVideoMessageDecoder: Decoder[ConfirmVideoMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
