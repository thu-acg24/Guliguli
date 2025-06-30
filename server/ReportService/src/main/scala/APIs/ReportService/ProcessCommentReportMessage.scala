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
 * ProcessCommentReportMessage
 * desc: 根据用户Token验证审核员权限后，更新评论举报表中对应记录的状态。
 * @param token: String (用户身份验证的令牌)
 * @param reportID: Int (举报记录的唯一标识符)
 * @param status: ReportStatus:1016 (举报记录的新状态)
 */

case class ProcessCommentReportMessage(
  token: String,
  reportID: Int,
  status: ReportStatus
) extends API[Unit](ReportServiceCode)

case object ProcessCommentReportMessage{

  private val circeEncoder: Encoder[ProcessCommentReportMessage] = deriveEncoder
  private val circeDecoder: Decoder[ProcessCommentReportMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ProcessCommentReportMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ProcessCommentReportMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ProcessCommentReportMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given processCommentReportMessageEncoder: Encoder[ProcessCommentReportMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given processCommentReportMessageDecoder: Decoder[ProcessCommentReportMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
