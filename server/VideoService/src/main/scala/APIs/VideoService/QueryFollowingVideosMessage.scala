package APIs.VideoService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.VideoServiceCode
import Objects.VideoService.Video
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * QueryFollowingVideosMessage
 * desc: 根据用户ID获取其发布的视频。
 * @param token: String (用户Token)
 * @return video: Video:1120 (用户发布的所有视频信息)
 */

case class QueryFollowingVideosMessage(
  token: String,
  fetchLimit: Int,
  lastTime: DateTime,
  lastID: Int
) extends API[List[Video]](VideoServiceCode)

case object QueryFollowingVideosMessage{

  private val circeEncoder: Encoder[QueryFollowingVideosMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryFollowingVideosMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryFollowingVideosMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryFollowingVideosMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryFollowingVideosMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given QueryFollowingVideosMessageEncoder: Encoder[QueryFollowingVideosMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given QueryFollowingVideosMessageDecoder: Decoder[QueryFollowingVideosMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
