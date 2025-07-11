package APIs.MessageService

import Common.API.API
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.MessageServiceCode
import Objects.MessageService.NoticesCount
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.*
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.joda.time.DateTime

import java.util.UUID
import scala.util.Try

/**
 * QueryNoticesCountMessage
 * desc: 根据用户Token验证身份后，查询当前用户和目标用户之间的私信记录。
 * @param token: String (用户的身份令牌，用于验证用户身份。)
 * @return noticesCount: NoticesCount (三种信息的未读个数。)
 */

case class QueryNoticesCountMessage(
  token: String
) extends API[NoticesCount](MessageServiceCode)

case object QueryNoticesCountMessage{

  private val circeEncoder: Encoder[QueryNoticesCountMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryNoticesCountMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryNoticesCountMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryNoticesCountMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryNoticesCountMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryNoticesCountMessageEncoder: Encoder[QueryNoticesCountMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryNoticesCountMessageDecoder: Decoder[QueryNoticesCountMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
