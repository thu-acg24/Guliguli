package Objects.VideoService

import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Objects.VideoService.VideoStatus
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * VideoAbstract
 * desc: 视频信息的数据结构
 * @param videoID: Int (视频的唯一标识)
 * @param title: String (视频的标题信息)
 * @param description: String (视频的描述信息)
 * @param duration: Option[Float] (视频的时长，单位为秒)
 * @param cover: Option[String] (视频封面的路径)
 * @param uploaderID: Int (上传视频的用户ID)
 * @param views: Int (视频的播放量)
 * @param likes: Int (视频的点赞数)
 * @param favorites: Int (视频的收藏数)
 * @param status: VideoStatus (视频的审核状态)
 * @param uploadTime: DateTime (视频的上传时间)
 */

case class VideoAbstract(
  videoID: Int,
  title: String,
  description: String,
  duration: Option[Float],
  cover: Option[String],
  uploaderID: Int,
  views: Int,
  likes: Int,
  favorites: Int,
  status: VideoStatus,
  uploadTime: DateTime
){

  //process class code 预留标志位，不要删除

}

case object VideoAbstract{

  private val circeEncoder: Encoder[VideoAbstract] = deriveEncoder
  private val circeDecoder: Decoder[VideoAbstract] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[VideoAbstract] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[VideoAbstract] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[VideoAbstract]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }

  // Circe + Jackson 兜底的 Encoder
  given videoEncoder: Encoder[VideoAbstract] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given videoDecoder: Decoder[VideoAbstract] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
