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
 * LoginMessage
 * desc: 用户传入昵称或邮箱和密码，校验登录信息，如果通过，则生成Token并返回。
 * @param usernameOrEmail: String (登录用户名或邮箱地址，用于进行身份查询。)
 * @param password: String (登录用户的明文密码，用于验证身份。)
 * @return token: String (登录结果，返回Token。)
 */

case class LoginMessage(
  usernameOrEmail: String,
  password: String
) extends API[String](UserServiceCode)

case object LoginMessage{

  private val circeEncoder: Encoder[LoginMessage] = deriveEncoder
  private val circeDecoder: Decoder[LoginMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[LoginMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[LoginMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[LoginMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given loginMessageEncoder: Encoder[LoginMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given loginMessageDecoder: Decoder[LoginMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
