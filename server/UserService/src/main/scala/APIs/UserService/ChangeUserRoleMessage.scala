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
 * ChangeUserRoleMessage
 * desc: 根据用户Token校验管理员权限后，指定用户成为审核员或将用户设为普通用户。
 * @param token: String (用户登录后获取的唯一身份标识，用于校验用户身份。)
 * @param userID: Int (目标用户的唯一标识符，用于指定需要更改角色的用户。)
 * @param newRole: UserRole:1084 (指定角色的新值，用于将用户设置为审核员或普通用户。)
 */

case class ChangeUserRoleMessage(
  token: String,
  userID: Int,
  newRole: UserRole
) extends API[Unit](UserServiceCode)

case object ChangeUserRoleMessage{

  private val circeEncoder: Encoder[ChangeUserRoleMessage] = deriveEncoder
  private val circeDecoder: Decoder[ChangeUserRoleMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ChangeUserRoleMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ChangeUserRoleMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ChangeUserRoleMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given changeUserRoleMessageEncoder: Encoder[ChangeUserRoleMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given changeUserRoleMessageDecoder: Decoder[ChangeUserRoleMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
