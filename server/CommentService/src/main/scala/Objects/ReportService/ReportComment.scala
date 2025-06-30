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
 * ReportComment
 * desc: 评论举报记录
 * @param reportID: Int (举报记录的唯一标识符)
 * @param commentID: Int (被举报的评论的唯一标识符)
 * @param reporterID: Int (举报人的唯一标识符)
 * @param reason: String (举报原因描述)
 * @param status: ReportStatus:1016 (举报记录的状态)
 */

case class ReportComment(
  reportID: Int,
  commentID: Int,
  reporterID: Int,
  reason: String,
  status: ReportStatus
){

  //process class code 预留标志位，不要删除

}

case object ReportComment{

  private val circeEncoder: Encoder[ReportComment] = deriveEncoder
  private val circeDecoder: Decoder[ReportComment] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ReportComment] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ReportComment] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ReportComment]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given reportCommentEncoder: Encoder[ReportComment] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given reportCommentDecoder: Decoder[ReportComment] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
