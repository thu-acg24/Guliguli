package Objects.MessageService

import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * Message
 * desc: 回复通知
 * @param noticeID: Int (通知的唯一ID)
 * @param senderID: Int (发送者的用户ID)
 * @param receiverID: Int (接收者的用户ID)
 * @param content: String (回复内容)
 * @param commentID: Int (回复ID)
 * @param originalContent: String (原评论内容)
 * @param originalCommentID: String (原评论ID)
 * @param timestamp: DateTime (消息发送的时间戳)
 */

case class ReplyNotice(
  noticeID: Int,
  senderID: Int,
  receiverID: Int,
  content: String,
  commentID: Int,
  originalContent: String,
  originalCommentID: Int,
  timestamp: DateTime,
){

  //process class code 预留标志位，不要删除

}

case object ReplyNotice{

  private val circeEncoder: Encoder[ReplyNotice] = deriveEncoder
  private val circeDecoder: Decoder[ReplyNotice] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ReplyNotice] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ReplyNotice] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ReplyNotice]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given messageEncoder: Encoder[ReplyNotice] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given messageDecoder: Decoder[ReplyNotice] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
