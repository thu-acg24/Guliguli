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
 * QueryLikedBatchMessage
 * desc: 根据commentID获取对应评论的详细信息
 * @param token: String (用户token，用来获取userID)
 * @param commentIds: List[Int] (评论ID列表)
 * @return liked: List[Boolean] (返回的boolean列表，表示用户是否点赞过该评论)
 */

case class QueryLikedBatchMessage(
  token: String,
  commentIds: List[Int]
) extends API[List[Boolean]](CommentServiceCode)

case object QueryLikedBatchMessage{

  private val circeEncoder: Encoder[QueryLikedBatchMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryLikedBatchMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryLikedBatchMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryLikedBatchMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryLikedBatchMessage]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }

  // Circe + Jackson 兜底的 Encoder
  given queryCommentByIDMessageEncoder: Encoder[QueryLikedBatchMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCommentByIDMessageDecoder: Decoder[QueryLikedBatchMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
