package Objects.VideoService


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
 * LikeRecord
 * desc: 用户点赞视频的记录
 * @param userID: Int (用户的唯一ID)
 * @param videoID: Int (视频的唯一ID)
 * @param timestamp: DateTime (点赞的时间戳)
 */

case class LikeRecord(
  userID: Int,
  videoID: Int,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除

}

case object LikeRecord{

  private val circeEncoder: Encoder[LikeRecord] = deriveEncoder
  private val circeDecoder: Decoder[LikeRecord] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[LikeRecord] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[LikeRecord] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[LikeRecord]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given likeRecordEncoder: Encoder[LikeRecord] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given likeRecordDecoder: Decoder[LikeRecord] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
