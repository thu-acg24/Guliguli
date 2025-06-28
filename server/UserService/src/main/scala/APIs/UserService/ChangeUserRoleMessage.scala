package APIs.UserService

import Common.API.API
import Global.ServiceCenter.UserServiceCode

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
import Objects.UserService.UserRole

/**
 * ChangeUserRoleMessage
 * desc: 根据用户Token校验管理员权限后，指定用户成为审核员或将用户设为普通用户。
 * @param token: String (用户登录后获取的唯一身份标识，用于校验用户身份。)
 * @param userID: Int (目标用户的唯一标识符，用于指定需要更改角色的用户。)
 * @param newRole: UserRole:1084 (指定角色的新值，用于将用户设置为审核员或普通用户。)
 * @return result: String (操作结果，返回错误信息或成功状态。)
 */

case class ChangeUserRoleMessage(
  token: String,
  userID: Int,
  newRole: UserRole
) extends API[Option[String]](UserServiceCode)



case object ChangeUserRoleMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
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

