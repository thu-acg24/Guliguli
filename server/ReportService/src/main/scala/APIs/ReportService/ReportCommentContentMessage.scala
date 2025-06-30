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
 * ReportCommentContentMessage
 * desc: 根据用户Token验证身份后，将举报记录保存到评论举报表。
 * @param token: String (用户的身份校验令牌)
 * @param commentID: Int (被举报的评论ID)
 * @param reason: String (举报理由)
 */

case class ReportCommentContentMessage(
  token: String,
  commentID: Int,
  reason: String
) extends API[Unit](ReportServiceCode)

case object ReportCommentContentMessage{

  private val circeEncoder: Encoder[ReportCommentContentMessage] = deriveEncoder
  private val circeDecoder: Decoder[ReportCommentContentMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ReportCommentContentMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ReportCommentContentMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ReportCommentContentMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given reportCommentContentMessageEncoder: Encoder[ReportCommentContentMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given reportCommentContentMessageDecoder: Decoder[ReportCommentContentMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
