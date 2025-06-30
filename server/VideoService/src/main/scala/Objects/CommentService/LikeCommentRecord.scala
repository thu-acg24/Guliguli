package Objects.CommentService


import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
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
 * LikeCommentRecord
 * desc: 用户点赞评论记录的数据结构
 * @param userID: Int (用户的唯一ID)
 * @param commentID: Int (评论的唯一ID)
 * @param timestamp: DateTime (记录点赞的时间戳)
 */

case class LikeCommentRecord(
  userID: Int,
  commentID: Int,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除

}

case object LikeCommentRecord{

  private val circeEncoder: Encoder[LikeCommentRecord] = deriveEncoder
  private val circeDecoder: Decoder[LikeCommentRecord] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[LikeCommentRecord] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[LikeCommentRecord] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[LikeCommentRecord]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given likeCommentRecordEncoder: Encoder[LikeCommentRecord] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given likeCommentRecordDecoder: Decoder[LikeCommentRecord] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
