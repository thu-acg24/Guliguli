package Objects.RecommendationService


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
