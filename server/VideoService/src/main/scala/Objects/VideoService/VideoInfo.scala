package Objects.VideoService


import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Objects.VideoService.VideoStatus
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
 * VideoInfo
 * desc: 视频信息的数据结构
 * @param videoID: Int (视频的唯一标识)
 * @param title: String (视频的标题信息)
 * @param description: String (视频的描述信息)
 * @param duration: Option[Float] (视频的时长，单位为秒)
 * @param tag: List[String] (视频的标签列表)
 * @param cover: Option[String] (视频封面的路径)
 * @param m3u8Path: Option[String] (视频索引的路径)
 * @param tsPrefix: Option[String] (视频分片的路径前缀)
 * @param sliceCount: Option[Int] (视频切片数)
 * @param tsPath: Option[List[String] ] (视频分片的路径前缀)
 * @param uploaderID: Int (上传视频的用户ID)
 * @param views: Int (视频的播放量)
 * @param likes: Int (视频的点赞数)
 * @param favorites: Int (视频的收藏数)
 * @param status: VideoStatus (视频的审核状态)
 * @param uploadTime: DateTime (视频的上传时间)
 */

case class VideoInfo(
  videoID: Int,
  title: String,
  description: String,
  duration: Option[Float],
  tag: List[String],
  cover: Option[String],
  m3u8Path: Option[String],
  tsPrefix: Option[String],
  sliceCount: Option[Int],
  tsPath: Option[List[String]],
  uploaderID: Int,
  views: Int,
  likes: Int,
  favorites: Int,
  status: VideoStatus,
  uploadTime: DateTime
){

  //process class code 预留标志位，不要删除

}

case object VideoInfo{

  private val circeEncoder: Encoder[VideoInfo] = deriveEncoder
  private val circeDecoder: Decoder[VideoInfo] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[VideoInfo] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[VideoInfo] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[VideoInfo]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given videoEncoder: Encoder[VideoInfo] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given videoDecoder: Decoder[VideoInfo] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
