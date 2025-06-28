package Objects.ReportService


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
 * ReportVideo
 * desc: 举报视频的数据结构，包含举报的基本信息
 * @param reportID: Int (举报记录的唯一标识)
 * @param videoID: Int (被举报视频的唯一标识)
 * @param reporterID: Int (举报用户的唯一标识)
 * @param reason: String (举报的原因)
 * @param status: ReportStatus:1016 (处理该举报的状态)
 */

case class ReportVideo(
  reportID: Int,
  videoID: Int,
  reporterID: Int,
  reason: String,
  status: ReportStatus
){

  //process class code 预留标志位，不要删除


}


case object ReportVideo{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ReportVideo] = deriveEncoder
  private val circeDecoder: Decoder[ReportVideo] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ReportVideo] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ReportVideo] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ReportVideo]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given reportVideoEncoder: Encoder[ReportVideo] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given reportVideoDecoder: Decoder[ReportVideo] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

