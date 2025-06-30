package APIs.CommentService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.CommentServiceCode
import Objects.CommentService.Comment
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
 * QueryVideoCommentsMessage
 * desc: 用于分页获取一个视频下的所有评论。
 * @param videoId: Int (视频的唯一标识符)
 * @param rangeL: Int (评论分页的开始范围)
 * @param rangeR: Int (评论分页的结束范围)
 * @return comments: Comment:1140 (查询到的评论列表，每个评论包含评论内容及相关信息)
 */

case class QueryVideoCommentsMessage(
  videoId: Int,
  rangeL: Int,
  rangeR: Int
) extends API[List[Comment]](CommentServiceCode)

case object QueryVideoCommentsMessage{

  private val circeEncoder: Encoder[QueryVideoCommentsMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryVideoCommentsMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryVideoCommentsMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryVideoCommentsMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryVideoCommentsMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryVideoCommentsMessageEncoder: Encoder[QueryVideoCommentsMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryVideoCommentsMessageDecoder: Decoder[QueryVideoCommentsMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
