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
 * QueryUploadPathMessage
 * desc: 根据用户Token校验身份后，通过给定的入参上传视频信息，并生成videoID存储到视频表。
 * @param token: String (用户身份校验的Token)
 * @param videoID: Int (视频ID)
 * @return uploadPath: UploadPath (上传路径)
 */

case class QueryUploadPathMessage(
  token: String,
  videoID: Int,
) extends API[UploadPath](VideoServiceCode)

case object QueryUploadPathMessage {

  private val circeEncoder: Encoder[QueryUploadPathMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryUploadPathMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryUploadPathMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryUploadPathMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces,
          new TypeReference[QueryUploadPathMessage]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryUploadPathMessageEncoder: Encoder[QueryUploadPathMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryUploadPathMessageDecoder: Decoder[QueryUploadPathMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
