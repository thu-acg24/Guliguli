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
 * FavoriteRecord
 * desc: 视频收藏记录，记录用户收藏视频的相关信息
 * @param userID: Int (用户的唯一ID)
 * @param videoID: Int (视频的唯一ID)
 * @param timestamp: DateTime (收藏视频的时间戳)
 */

case class FavoriteRecord(
  userID: Int,
  videoID: Int,
  timestamp: DateTime
){

  //process class code 预留标志位，不要删除

}

case object FavoriteRecord{

  private val circeEncoder: Encoder[FavoriteRecord] = deriveEncoder
  private val circeDecoder: Decoder[FavoriteRecord] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[FavoriteRecord] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[FavoriteRecord] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[FavoriteRecord]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given favoriteRecordEncoder: Encoder[FavoriteRecord] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given favoriteRecordDecoder: Decoder[FavoriteRecord] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
