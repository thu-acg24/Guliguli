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
 * ChangeBanStatusMessage
 * desc: 根据用户Token校验审核员权限后，封禁或解封指定用户并更新用户表状态。用于修改用户封禁状态功能点
 * @param token: String (用户身份验证的Token，用于校验身份及权限。)
 * @param userID: Int (需要修改封禁状态的目标用户ID。)
 * @param isBan: Boolean (标识是否封禁用户，true为封禁，false为解封。)
 */

case class ChangeBanStatusMessage(
  token: String,
  userID: Int,
  isBan: Boolean
) extends API[Unit](UserServiceCode)

case object ChangeBanStatusMessage{

  private val circeEncoder: Encoder[ChangeBanStatusMessage] = deriveEncoder
  private val circeDecoder: Decoder[ChangeBanStatusMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ChangeBanStatusMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ChangeBanStatusMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ChangeBanStatusMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given ChangeBanStatusMessageEncoder: Encoder[ChangeBanStatusMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given ChangeBanStatusMessageDecoder: Decoder[ChangeBanStatusMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
