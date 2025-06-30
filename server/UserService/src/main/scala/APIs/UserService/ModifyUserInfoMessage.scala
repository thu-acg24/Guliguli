package APIs.UserService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.UserServiceCode
import Objects.UserService.UserInfo
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
 * ModifyUserInfoMessage
 * desc: 根据用户Token校验身份后，更新用户表中newField对应字段的值，用于用户信息修改功能点
 * @param token: String (用户的身份验证Token)
 * @param newField: UserInfo:1100 (需要更新的用户字段值)
 */

case class ModifyUserInfoMessage(
  token: String,
  newField: UserInfo
) extends API[Unit](UserServiceCode)

case object ModifyUserInfoMessage{

  private val circeEncoder: Encoder[ModifyUserInfoMessage] = deriveEncoder
  private val circeDecoder: Decoder[ModifyUserInfoMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ModifyUserInfoMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ModifyUserInfoMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ModifyUserInfoMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given modifyUserInfoMessageEncoder: Encoder[ModifyUserInfoMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given modifyUserInfoMessageDecoder: Decoder[ModifyUserInfoMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
