package APIs.VideoService

import Common.API.API
import Global.ServiceCenter.VideoServiceCode

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
 * UploadVideoMessage
 * desc: 根据用户Token校验身份后，通过给定的入参上传视频信息，并生成videoID存储到视频表。
 * @param token: String (用户身份校验的Token)
 * @param videoPath: String (视频存储路径)
 * @param title: String (视频标题)
 * @param coverPath: String (视频封面路径)
 * @param description: String (视频简介)
 * @param tag: String (视频标签列表)
 * @param duration: Int (视频时长（秒）)
 * @return result: String (操作结果，成功返回None，失败返回错误信息)
 */

case class UploadVideoMessage(
  token: String,
  videoPath: String,
  title: String,
  coverPath: String,
  description: String,
  tag: List[String],
  duration: Int
) extends API[Unit](VideoServiceCode)



case object UploadVideoMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UploadVideoMessage] = deriveEncoder
  private val circeDecoder: Decoder[UploadVideoMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UploadVideoMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UploadVideoMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UploadVideoMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given uploadVideoMessageEncoder: Encoder[UploadVideoMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given uploadVideoMessageDecoder: Decoder[UploadVideoMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

