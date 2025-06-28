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
 * ModifyVideoMessage
 * desc: 根据用户Token校验权限后，修改视频表中指定字段的值。
 * @param token: String (用户认证Token，用于校验身份)
 * @param videoID: Int (目标视频的唯一标识ID)
 * @param videoPath: String (视频存储的路径（可选）)
 * @param title: String (视频标题（可选）)
 * @param coverPath: String (视频封面的路径（可选）)
 * @param description: String (视频描述（可选）)
 * @param tag: String (视频标签列表（可选）)
 * @param duration: Int (视频时长（可选）)
 * @return result: String (修改结果消息，返回None表示修改成功，返回Some[String]表示失败原因)
 */

case class ModifyVideoMessage(
  token: String,
  videoID: Int,
  videoPath: Option[String] = None,
  title: Option[String] = None,
  coverPath: Option[String] = None,
  description: Option[String] = None,
  tag: List[String],
  duration: Option[Int] = None
) extends API[Option[String]](VideoServiceCode)



case object ModifyVideoMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ModifyVideoMessage] = deriveEncoder
  private val circeDecoder: Decoder[ModifyVideoMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ModifyVideoMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ModifyVideoMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ModifyVideoMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given modifyVideoMessageEncoder: Encoder[ModifyVideoMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given modifyVideoMessageDecoder: Decoder[ModifyVideoMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

