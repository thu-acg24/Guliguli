package APIs.MessageService

import Common.API.API
import Global.ServiceCenter.MessageServiceCode

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
import Objects.UserService.UserInfo

/**
 * QueryUserInContactMessage
 * desc: 根据用户Token验证身份后，查询所有与当前用户有私信记录（发送或接收）的联系人列表。
 * @param token: String (用户的身份验证令牌。)
 * @return contacts: UserInfo:1100 (与当前用户有私信联系的用户信息列表，包括基本信息如昵称与头像路径。)
 */

case class QueryUserInContactMessage(
  token: String
) extends API[List[UserInfo]](MessageServiceCode)



case object QueryUserInContactMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryUserInContactMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryUserInContactMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryUserInContactMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryUserInContactMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryUserInContactMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryUserInContactMessageEncoder: Encoder[QueryUserInContactMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryUserInContactMessageDecoder: Decoder[QueryUserInContactMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

