package APIs.UserService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.UserServiceCode
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * ValidateAvatarMessage
 * desc: 用户上传完头像后，将上一步得到的Token传入，用于检验用户上传文件是否合法功能
 * @param token: String (sessionToken，用于校验身份及权限。)
 */

case class ValidateAvatarMessage(
  token: String
) extends API[List[String]](UserServiceCode)

case object ValidateAvatarMessage{

  private val circeEncoder: Encoder[ValidateAvatarMessage] = deriveEncoder
  private val circeDecoder: Decoder[ValidateAvatarMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ValidateAvatarMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ValidateAvatarMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ValidateAvatarMessage]() {})) }
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given ValidateAvatarMessageEncoder: Encoder[ValidateAvatarMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given ValidateAvatarMessageDecoder: Decoder[ValidateAvatarMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
