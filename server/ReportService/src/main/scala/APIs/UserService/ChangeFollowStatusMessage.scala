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
 * ChangeFollowStatusMessage
 * desc: 创建或删除当前用户和目标用户之间的关注关系, 用于用户关注或取消关注功能点
 * @param token: String (用户登录的Token，用于校验身份)
 * @param followeeID: Int (被关注的目标用户ID)
 * @param isFollow: Boolean (操作类型，true表示关注，false表示取消关注)
 * @return result: String (操作结果，返回None表示成功，Some(String)表示失败的错误信息)
 */

case class ChangeFollowStatusMessage(
  token: String,
  followeeID: Int,
  isFollow: Boolean
) extends API[Option[String]](UserServiceCode)



case object ChangeFollowStatusMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[ChangeFollowStatusMessage] = deriveEncoder
  private val circeDecoder: Decoder[ChangeFollowStatusMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[ChangeFollowStatusMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[ChangeFollowStatusMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[ChangeFollowStatusMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given changeFollowStatusMessageEncoder: Encoder[ChangeFollowStatusMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given changeFollowStatusMessageDecoder: Decoder[ChangeFollowStatusMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

