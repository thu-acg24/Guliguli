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
 * UpdateLikeCommentMessage
 * desc: 用于点赞或取消点赞指定评论。
 * @param token: String (用户的身份认证令牌，用于校验用户身份。)
 * @param commentID: Int (目标评论的唯一标识符。)
 * @param isLike: Boolean (标记用户的操作类型，true表示点赞，false表示取消点赞。)
 */

case class UpdateLikeCommentMessage(
  token: String,
  commentID: Int,
  isLike: Boolean
) extends API[Unit](CommentServiceCode)

case object UpdateLikeCommentMessage{

  private val circeEncoder: Encoder[UpdateLikeCommentMessage] = deriveEncoder
  private val circeDecoder: Decoder[UpdateLikeCommentMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateLikeCommentMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateLikeCommentMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateLikeCommentMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateLikeCommentMessageEncoder: Encoder[UpdateLikeCommentMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateLikeCommentMessageDecoder: Decoder[UpdateLikeCommentMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
