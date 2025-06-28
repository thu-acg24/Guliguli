package Objects.RecommendationService


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
 * FeedbackDetail
 * desc: 用户反馈详情
 * @param feedbackID: Int (反馈的唯一标识符)
 * @param userID: Int (用户的唯一标识符)
 * @param like: Boolean (用户是否喜欢该内容)
 * @param favorite: Boolean (用户是否收藏该内容)
 * @param timestamp: DateTime (反馈产生的时间戳)
 */

case class FeedbackDetail(
  feedbackID: Int,
  userID: Int,
  like: Boolean,
  favorite: Boolean,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除


}


case object FeedbackDetail{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[FeedbackDetail] = deriveEncoder
  private val circeDecoder: Decoder[FeedbackDetail] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[FeedbackDetail] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[FeedbackDetail] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[FeedbackDetail]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given feedbackDetailEncoder: Encoder[FeedbackDetail] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given feedbackDetailDecoder: Decoder[FeedbackDetail] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

