package Objects.UserService


import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
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
 * UserInfo
 * desc: 用户信息，包含基本的用户名、头像和封禁状态
 * @param userID: Int (用户的唯一ID)
 * @param username: String (用户名)
 * @param avatarPath: String (用户头像的存储路径)
 * @param isBanned: Boolean (用户是否被封禁)
 */

case class UserInfo(
  userID: Int,
  username: String,
  avatarPath: String,
  isBanned: Boolean
){

  //process class code 预留标志位，不要删除

}

case object UserInfo{

  private val circeEncoder: Encoder[UserInfo] = deriveEncoder
  private val circeDecoder: Decoder[UserInfo] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UserInfo] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UserInfo] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UserInfo]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given userInfoEncoder: Encoder[UserInfo] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given userInfoDecoder: Decoder[UserInfo] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  //process object code 预留标志位，不要删除

}
