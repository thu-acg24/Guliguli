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
 * QueryAuditorsMessage
 * desc: 根据用户Token校验管理员权限后，查询所有审核员的列表。
 * @param token: String (用户身份标识的令牌，用于校验当前登录用户身份。)
 * @return auditors: UserInfo:1100 (审核员的列表信息。)
 */

case class QueryAuditorsMessage(
  token: String
) extends API[List[UserInfo]](UserServiceCode)

case object QueryAuditorsMessage{

  private val circeEncoder: Encoder[QueryAuditorsMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryAuditorsMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryAuditorsMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryAuditorsMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryAuditorsMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryAuditorsMessageEncoder: Encoder[QueryAuditorsMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryAuditorsMessageDecoder: Decoder[QueryAuditorsMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
