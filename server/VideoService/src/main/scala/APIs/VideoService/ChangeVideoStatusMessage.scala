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
import Objects.VideoService.VideoStatus

/**
 * ChangeVideoStatusMessage
 * desc: 修改视频审核状态
 * @param token: String (用户身份凭证，用于校验权限)
 * @param videoID: Int (视频ID，用于定位要修改状态的视频)
 * @param status: VideoStatus:1022 (目标审核状态，例如待审核、审核通过或审核拒绝)
 * @return result: String (操作结果，返回错误信息或空值表示成功)
 */

case class ChangeVideoStatusMessage(
  token: String,
  videoID: Int,
  status: VideoStatus
) extends API[Option[String]](VideoServiceCode)



case object ChangeVideoStatusMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ChangeVideoStatusMessage] = deriveEncoder
  private val circeDecoder: Decoder[ChangeVideoStatusMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ChangeVideoStatusMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ChangeVideoStatusMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ChangeVideoStatusMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given changeVideoStatusMessageEncoder: Encoder[ChangeVideoStatusMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given changeVideoStatusMessageDecoder: Decoder[ChangeVideoStatusMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

