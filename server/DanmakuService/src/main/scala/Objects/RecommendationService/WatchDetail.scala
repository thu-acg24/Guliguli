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
 * WatchDetail
 * desc: 记录用户观看视频的详细信息
 * @param watchID: Int (观看记录的唯一标识)
 * @param userID: Int (与观看行为相关的用户的ID)
 * @param videoID: Int (观看的视频的唯一标识)
 * @param watchDuration: Float (用户观看视频的时长（秒数）)
 * @param timestamp: DateTime (观看行为发生的时间)
 */

case class WatchDetail(
  watchID: Int,
  userID: Int,
  videoID: Int,
  watchDuration: Float,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除


}


case object WatchDetail{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[WatchDetail] = deriveEncoder
  private val circeDecoder: Decoder[WatchDetail] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[WatchDetail] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[WatchDetail] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[WatchDetail]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given watchDetailEncoder: Encoder[WatchDetail] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given watchDetailDecoder: Decoder[WatchDetail] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

