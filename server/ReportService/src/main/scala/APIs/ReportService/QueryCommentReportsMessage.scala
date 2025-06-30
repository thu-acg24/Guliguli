package APIs.ReportService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.ReportServiceCode
import Objects.ReportService.ReportComment
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
 * QueryCommentReportsMessage
 * desc: 根据用户Token验证审核员权限后，查询评论举报记录。
 * @param token: String (用户身份验证令牌，用于校验用户。)
 * @return reports: ReportComment:1170 (查询结果，包含所有待处理的评论举报记录。)
 */

case class QueryCommentReportsMessage(
  token: String
) extends API[List[ReportComment]](ReportServiceCode)

case object QueryCommentReportsMessage{

  private val circeEncoder: Encoder[QueryCommentReportsMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCommentReportsMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCommentReportsMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCommentReportsMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCommentReportsMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryCommentReportsMessageEncoder: Encoder[QueryCommentReportsMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCommentReportsMessageDecoder: Decoder[QueryCommentReportsMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
