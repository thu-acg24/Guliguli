package APIs.CommentService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.CommentServiceCode
import Objects.CommentService.Comment
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * QueryCommentCountMessage
 * desc: 根据commentID获取对应评论的详细信息
 * @param commentID: Int (评论ID，用于唯一标识某个评论)
 * @return comment: Comment:1140 (返回的评论对象，包含评论的详细信息)
 */

case class QueryCommentCountMessage(
  commentID: Int
) extends API[Comment](CommentServiceCode)

case object QueryCommentCountMessage{

  private val circeEncoder: Encoder[QueryCommentCountMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCommentCountMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCommentCountMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCommentCountMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCommentCountMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given QueryCommentCountMessageEncoder: Encoder[QueryCommentCountMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given QueryCommentCountMessageDecoder: Decoder[QueryCommentCountMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
