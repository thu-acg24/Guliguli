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


/**
 * ChangeBanStatusMessage
 * desc: 根据用户Token校验审核员权限后，封禁或解封指定用户并更新用户表状态。用于修改用户封禁状态功能点
 * @param token: String (用户身份验证的Token，用于校验身份及权限。)
 * @param userID: Int (需要修改封禁状态的目标用户ID。)
 * @param isBan: Boolean (标识是否封禁用户，true为封禁，false为解封。)
 * @return result: String (操作结果，返回None表示成功，否则为具体错误信息。)
 */

case class ChangeBanStatusMessage(
  token: String,
  userID: Int,
  isBan: Boolean
) extends API[Option[String]](UserServiceCode)



case object ChangeBanStatusMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
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

