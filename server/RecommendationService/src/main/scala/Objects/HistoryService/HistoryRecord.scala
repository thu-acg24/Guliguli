package Objects.HistoryService


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
 * HistoryRecord
 * desc: 用户观看历史记录
 * @param userID: Int (用户的唯一ID)
 * @param videoID: Int (视频的唯一ID)
 * @param timestamp: DateTime (观看时间的时间戳)
 */

case class HistoryRecord(
  userID: Int,
  videoID: Int,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除

}

case object HistoryRecord{

  private val circeEncoder: Encoder[HistoryRecord] = deriveEncoder
  private val circeDecoder: Decoder[HistoryRecord] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[HistoryRecord] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[HistoryRecord] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[HistoryRecord]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given historyRecordEncoder: Encoder[HistoryRecord] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given historyRecordDecoder: Decoder[HistoryRecord] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
