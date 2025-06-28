package Objects.CommentService


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
 * Comment
 * desc: 评论详情
 * @param commentID: Int (评论的唯一标识符)
 * @param content: String (评论的内容)
 * @param videoID: Int (视频的唯一标识符，被评论的视频)
 * @param authorID: Int (评论作者的唯一标识符)
 * @param replyToID: Int (被回复的评论ID, 如果为空则表示不是回复)
 * @param likes: Int (点赞数)
 * @param timestamp: DateTime (评论创建的时间戳)
 */

case class Comment(
  commentID: Int,
  content: String,
  videoID: Int,
  authorID: Int,
  replyToID: Option[Int] = None,
  likes: Int,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除


}


case object Comment{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[Comment] = deriveEncoder
  private val circeDecoder: Decoder[Comment] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[Comment] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[Comment] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[Comment]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given commentEncoder: Encoder[Comment] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given commentDecoder: Decoder[Comment] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

