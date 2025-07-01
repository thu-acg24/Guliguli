package APIs.VideoService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.VideoServiceCode
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
 */

case class ModifyVideoMessage(
  token: String,
  videoID: Int,
  videoPath: Option[String] = None,
  title: Option[String] = None,
  coverPath: Option[String] = None,
  description: Option[String] = None,
  tag: Option[List[String]] = None,
  duration: Option[Int] = None
) extends API[Unit](VideoServiceCode)

case object ModifyVideoMessage{

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
