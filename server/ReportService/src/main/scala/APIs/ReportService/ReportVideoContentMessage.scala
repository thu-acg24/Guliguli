package APIs.ReportService

import Common.API.API
import Global.ServiceCenter.ReportServiceCode

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
 * ReportVideoContentMessage
 * desc: 根据用户Token检验身份后，将举报记录保存到视频举报表。
 * @param token: String (用户登录认证Token，用于识别用户身份。)
 * @param videoID: Int (被举报的视频ID。)
 * @param reason: String (举报理由。)
 */

case class ReportVideoContentMessage(
  token: String,
  videoID: Int,
  reason: String
) extends API[Unit](ReportServiceCode)



case object ReportVideoContentMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ReportVideoContentMessage] = deriveEncoder
  private val circeDecoder: Decoder[ReportVideoContentMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ReportVideoContentMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ReportVideoContentMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ReportVideoContentMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given reportVideoContentMessageEncoder: Encoder[ReportVideoContentMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given reportVideoContentMessageDecoder: Decoder[ReportVideoContentMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

