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
 * RegisterMessage
 * desc: 用户传入昵称、邮箱以及密码，计算密码哈希，生成userID并存储到用户表，同时初始化用户角色为普通用户，用于用户注册功能点
 * @param username: String (用户的用户名，用于注册身份标识)
 * @param email: String (用户的邮箱，用于注册身份标识)
 * @param password: String (明文密码，用于注册时计算用户的密码哈希值)
 */

case class RegisterMessage(
  username: String,
  email: String,
  password: String
) extends API[Unit](UserServiceCode)

case object RegisterMessage{

  private val circeEncoder: Encoder[RegisterMessage] = deriveEncoder
  private val circeDecoder: Decoder[RegisterMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[RegisterMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[RegisterMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[RegisterMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given registerMessageEncoder: Encoder[RegisterMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given registerMessageDecoder: Decoder[RegisterMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
