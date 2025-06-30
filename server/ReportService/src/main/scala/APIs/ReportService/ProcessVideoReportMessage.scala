package APIs.ReportService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.ReportServiceCode
import Objects.ReportService.ReportStatus
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
 * ProcessVideoReportMessage
 * desc: 根据用户Token验证审核员权限后，更新视频举报表中对应记录的状态
 * @param token: String (用户认证的Token，用于校验用户身份和权限。)
 * @param reportID: Int (举报记录的唯一标识ID。)
 * @param status: ReportStatus:1016 (举报记录的新状态，例如待处理、已处理或驳回。)
 */

case class ProcessVideoReportMessage(
  token: String,
  reportID: Int,
  status: ReportStatus
) extends API[Unit](ReportServiceCode)

case object ProcessVideoReportMessage{

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
