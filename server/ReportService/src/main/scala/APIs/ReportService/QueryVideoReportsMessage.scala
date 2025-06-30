package APIs.ReportService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.ReportServiceCode
import Objects.ReportService.ReportVideo
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
 * QueryVideoReportsMessage
 * desc: 根据用户Token验证审核员权限后，查询视频举报记录。
 * @param token: String (用户认证的Token，用于权限校验)
 * @return reports: ReportVideo:1068 (视频举报记录的列表，每条记录包含举报详情字段)
 */

case class QueryVideoReportsMessage(
  token: String
) extends API[List[ReportVideo]](ReportServiceCode)

case object QueryVideoReportsMessage{

  private val circeEncoder: Encoder[QueryVideoReportsMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryVideoReportsMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryVideoReportsMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryVideoReportsMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryVideoReportsMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryVideoReportsMessageEncoder: Encoder[QueryVideoReportsMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryVideoReportsMessageDecoder: Decoder[QueryVideoReportsMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
