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
import Objects.ReportService.ReportStatus

/**
 * ProcessVideoReportMessage
 * desc: 根据用户Token验证审核员权限后，更新视频举报表中对应记录的状态
 * @param token: String (用户认证的Token，用于校验用户身份和权限。)
 * @param reportID: Int (举报记录的唯一标识ID。)
 * @param status: ReportStatus:1016 (举报记录的新状态，例如待处理、已处理或驳回。)
 * @return result: String (操作结果，返回空值代表成功，或返回错误信息。)
 */

case class ProcessVideoReportMessage(
  token: String,
  reportID: Int,
  status: ReportStatus
) extends API[String](ReportServiceCode)



case object ProcessVideoReportMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ProcessVideoReportMessage] = deriveEncoder
  private val circeDecoder: Decoder[ProcessVideoReportMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ProcessVideoReportMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ProcessVideoReportMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ProcessVideoReportMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given processVideoReportMessageEncoder: Encoder[ProcessVideoReportMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given processVideoReportMessageDecoder: Decoder[ProcessVideoReportMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

