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
 * QueryUserInfoMessage
 * desc: 根据用户ID查询用户表，返回用户的基本信息。
 * @param userID: Int (用户的唯一标识)
 * @return user: UserInfo:1100 (查询返回的用户基本信息，包括ID、用户名、头像路径和封禁状态)
 */

case class QueryUserInfoMessage(
  userID: Int
) extends API[UserInfo](UserServiceCode)

case object QueryUserInfoMessage{

  private val circeEncoder: Encoder[QueryUserInfoMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryUserInfoMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryUserInfoMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryUserInfoMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryUserInfoMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryUserInfoMessageEncoder: Encoder[QueryUserInfoMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryUserInfoMessageDecoder: Decoder[QueryUserInfoMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
