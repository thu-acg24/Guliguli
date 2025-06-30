package APIs.ReportService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.ReportServiceCode
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
 * ReportDanmakuContentMessage
 * desc: 根据用户Token验证身份后，将举报记录保存到弹幕举报表。
 * @param token: String (用户身份的认证令牌。)
 * @param danmakuID: Int (被举报的弹幕的唯一标识。)
 * @param reason: String (举报该弹幕的理由。)
 */

case class ReportDanmakuContentMessage(
  token: String,
  danmakuID: Int,
  reason: String
) extends API[Unit](ReportServiceCode)

case object ReportDanmakuContentMessage{

  private val circeEncoder: Encoder[ReportDanmakuContentMessage] = deriveEncoder
  private val circeDecoder: Decoder[ReportDanmakuContentMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ReportDanmakuContentMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ReportDanmakuContentMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ReportDanmakuContentMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given reportDanmakuContentMessageEncoder: Encoder[ReportDanmakuContentMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given reportDanmakuContentMessageDecoder: Decoder[ReportDanmakuContentMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
