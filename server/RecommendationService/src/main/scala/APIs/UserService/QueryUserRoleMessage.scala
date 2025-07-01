package APIs.UserService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.UserServiceCode
import Objects.UserService.UserRole
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
 * QueryUserRoleMessage
 * desc: 根据用户Token校验身份后，返回当前用户的基本信息。
 * @param token: String (用户的验证Token，用于校验用户身份。)
 * @return userRole: UserRole:1084 (用户的角色信息，如管理员、审核员或普通用户。)
 */

case class QueryUserRoleMessage(
  token: String
) extends API[UserRole](UserServiceCode)

case object QueryUserRoleMessage{

  private val circeEncoder: Encoder[QueryUserRoleMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryUserRoleMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryUserRoleMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryUserRoleMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryUserRoleMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryUserRoleMessageEncoder: Encoder[QueryUserRoleMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryUserRoleMessageDecoder: Decoder[QueryUserRoleMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
