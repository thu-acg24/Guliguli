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
 * ProcessDanmakuReportMessage
 * desc: 根据用户Token验证审核员权限后，更新弹幕举报表中对应记录的状态。
 * @param token: String (用户认证Token，用于身份校验)
 * @param reportID: Int (举报记录的ID，用于标识具体举报记录)
 * @param status: ReportStatus:1016 (要更新的举报状态)
 */

case class ProcessDanmakuReportMessage(
  token: String,
  reportID: Int,
  status: ReportStatus
) extends API[Unit](ReportServiceCode)



case object ProcessDanmakuReportMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ProcessDanmakuReportMessage] = deriveEncoder
  private val circeDecoder: Decoder[ProcessDanmakuReportMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ProcessDanmakuReportMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ProcessDanmakuReportMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ProcessDanmakuReportMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given processDanmakuReportMessageEncoder: Encoder[ProcessDanmakuReportMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given processDanmakuReportMessageDecoder: Decoder[ProcessDanmakuReportMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

