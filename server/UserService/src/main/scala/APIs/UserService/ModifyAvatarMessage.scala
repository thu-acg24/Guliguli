package APIs.UserService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.UserServiceCode
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * ModifyAvatarMessage
 * desc: 根据用户Token校验审核员权限后，封禁或解封指定用户并更新用户表状态。用于修改用户封禁状态功能点
 * @param token: String (用户身份验证的Token，用于校验身份及权限。)
 * @return result: String (上传URL)
 */

case class ModifyAvatarMessage(
  token: String
) extends API[List[String]](UserServiceCode)

case object ModifyAvatarMessage{

  private val circeEncoder: Encoder[ModifyAvatarMessage] = deriveEncoder
  private val circeDecoder: Decoder[ModifyAvatarMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ModifyAvatarMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ModifyAvatarMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ModifyAvatarMessage]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given ChangeBanStatusMessageEncoder: Encoder[ModifyAvatarMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given ChangeBanStatusMessageDecoder: Decoder[ModifyAvatarMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
