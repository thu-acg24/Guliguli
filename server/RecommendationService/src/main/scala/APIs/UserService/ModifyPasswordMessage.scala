package APIs.UserService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.UserServiceCode
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
 * ModifyPasswordMessage
 * desc: 根据用户Token和旧密码校验身份后，更新用户表中密码哈希的值，用于用户信息修改功能点。
 * @param token: String (用户的身份验证令牌)
 * @param oldPassword: String (用户的原始密码，用于验证身份)
 * @param newPassword: String (用户的新密码，用于更新密码哈希)
 */

case class ModifyPasswordMessage(
  token: String,
  oldPassword: String,
  newPassword: String
) extends API[Unit](UserServiceCode)

case object ModifyPasswordMessage{

  private val circeEncoder: Encoder[ModifyPasswordMessage] = deriveEncoder
  private val circeDecoder: Decoder[ModifyPasswordMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ModifyPasswordMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ModifyPasswordMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ModifyPasswordMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given modifyPasswordMessageEncoder: Encoder[ModifyPasswordMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given modifyPasswordMessageDecoder: Decoder[ModifyPasswordMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
