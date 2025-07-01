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
 * ChangeFollowStatusMessage
 * desc: 创建或删除当前用户和目标用户之间的关注关系, 用于用户关注或取消关注功能点
 * @param token: String (用户登录的Token，用于校验身份)
 * @param followeeID: Int (被关注的目标用户ID)
 * @param isFollow: Boolean (操作类型，true表示关注，false表示取消关注)
 */

case class ChangeFollowStatusMessage(
  token: String,
  followeeID: Int,
  isFollow: Boolean
) extends API[Unit](UserServiceCode)

case object ChangeFollowStatusMessage{

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
