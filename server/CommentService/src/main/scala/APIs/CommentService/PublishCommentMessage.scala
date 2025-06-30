package APIs.CommentService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.CommentServiceCode
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
 * PublishCommentMessage
 * desc: 用于发布新的评论或回复评论。
 * @param token: String (用户的身份令牌，用于验证用户身份。)
 * @param videoID: Int (需要评论的视频的ID。)
 * @param commentContent: String (评论的具体内容。)
 * @param replyToCommentID: Int (回复的目标评论ID，可空。)
 */

case class PublishCommentMessage(
  token: String,
  videoID: Int,
  commentContent: String,
  replyToCommentID: Option[Int] = None
) extends API[Unit](CommentServiceCode)

case object PublishCommentMessage{

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
