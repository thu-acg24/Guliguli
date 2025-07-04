package APIs.UserService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.UserServiceCode
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * ConfirmAvatarMessage
 * desc: Worker 内部调用，确认上传路径
 * @param token: String (用户身份验证的Token，用于校验身份及权限。)
 * @param objectName: String (文件名)
 */

case class ConfirmAvatarMessage(
  token: String,
  status: String,
  objectName: String
) extends API[Unit](UserServiceCode)

case object ConfirmAvatarMessage{

  private val circeEncoder: Encoder[ConfirmAvatarMessage] = deriveEncoder
  private val circeDecoder: Decoder[ConfirmAvatarMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ConfirmAvatarMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ConfirmAvatarMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ConfirmAvatarMessage]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given confirmAvatarMessageEncoder: Encoder[ConfirmAvatarMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given confirmAvatarMessageDecoder: Decoder[ConfirmAvatarMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
