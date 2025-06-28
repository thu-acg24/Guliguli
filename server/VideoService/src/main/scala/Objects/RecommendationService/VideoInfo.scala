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
 * VideoInfo
 * desc: 视频的基本信息
 * @param videoID: Int (视频的唯一ID)
 * @param title: String (视频的标题)
 * @param description: String (视频的描述)
 * @param tag: String (视频的标签列表)
 * @param uploaderID: Int (上传者的用户ID)
 * @param views: Int (视频的观看数量)
 * @param likes: Int (视频的点赞数量)
 * @param favorites: Int (视频的收藏数量)
 * @param visible: Boolean (视频是否可见)
 */

case class VideoInfo(
  videoID: Int,
  title: String,
  description: String,
  tag: List[String],
  uploaderID: Int,
  views: Int,
  likes: Int,
  favorites: Int,
  visible: Boolean
){

  //process class code 预留标志位，不要删除


}


case object VideoInfo{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
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
  given videoInfoEncoder: Encoder[VideoInfo] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given videoInfoDecoder: Decoder[VideoInfo] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

