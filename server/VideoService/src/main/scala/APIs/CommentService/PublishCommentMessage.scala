package APIs.CommentService

import Common.API.API
import Global.ServiceCenter.CommentServiceCode

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


/**
 * PublishCommentMessage
 * desc: 用于发布新的评论或回复评论。
 * @param token: String (用户的身份令牌，用于验证用户身份。)
 * @param videoID: Int (需要评论的视频的ID。)
 * @param commentContent: String (评论的具体内容。)
 * @param replyToCommentID: Int (回复的目标评论ID，可空。)
 * @return result: String (返回操作的结果，如果成功返回None，否则返回错误信息。)
 */

case class PublishCommentMessage(
  token: String,
  videoID: Int,
  commentContent: String,
  replyToCommentID: Option[Int] = None
) extends API[Option[String]](CommentServiceCode)



case object PublishCommentMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[PublishCommentMessage] = deriveEncoder
  private val circeDecoder: Decoder[PublishCommentMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[PublishCommentMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[PublishCommentMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[PublishCommentMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given publishCommentMessageEncoder: Encoder[PublishCommentMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given publishCommentMessageDecoder: Decoder[PublishCommentMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

