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
import Objects.ReportService.ReportDanmaku

/**
 * QueryDanmakuReportsMessage
 * desc: 根据用户Token验证审核员权限后，查询弹幕举报记录。
 * @param token: String (用户身份验证令牌，用于校验用户的身份和权限。)
 * @return reports: ReportDanmaku:1053 (包含所有待处理的弹幕举报记录的列表，每条记录包括举报的详细信息。)
 */

case class QueryDanmakuReportsMessage(
  token: String
) extends API[List[ReportDanmaku]](ReportServiceCode)



case object QueryDanmakuReportsMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryDanmakuReportsMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryDanmakuReportsMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryDanmakuReportsMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryDanmakuReportsMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryDanmakuReportsMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryDanmakuReportsMessageEncoder: Encoder[QueryDanmakuReportsMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryDanmakuReportsMessageDecoder: Decoder[QueryDanmakuReportsMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

