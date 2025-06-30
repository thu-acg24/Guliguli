package Objects.ReportService


import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
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
 * ReportDanmaku
 * desc: 弹幕举报信息
 * @param reportID: Int (举报记录的唯一ID)
 * @param danmakuID: Int (被举报的弹幕的唯一ID)
 * @param reporterID: Int (举报人的唯一ID)
 * @param reason: String (举报理由)
 * @param status: ReportStatus:1016 (举报处理状态)
 */

case class ReportDanmaku(
  reportID: Int,
  danmakuID: Int,
  reporterID: Int,
  reason: String,
  status: ReportStatus
){

  //process class code 预留标志位，不要删除

}

case object ReportDanmaku{

  private val circeEncoder: Encoder[ReportDanmaku] = deriveEncoder
  private val circeDecoder: Decoder[ReportDanmaku] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ReportDanmaku] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ReportDanmaku] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ReportDanmaku]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given reportDanmakuEncoder: Encoder[ReportDanmaku] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given reportDanmakuDecoder: Decoder[ReportDanmaku] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
