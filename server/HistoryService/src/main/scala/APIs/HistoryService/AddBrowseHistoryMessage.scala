package APIs.HistoryService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.HistoryServiceCode
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
 * AddBrowseHistoryMessage
 * desc: 根据用户Token校验后，记录用户浏览的视频到历史记录表。
 * @param token: String (用户的身份验证信息，用于校验用户身份是否合法。)
 * @param videoID: Int (视频的唯一标识符，用于指定用户浏览的视频。)
 */

case class AddBrowseHistoryMessage(
  token: String,
  videoID: Int
) extends API[Unit](HistoryServiceCode)

case object AddBrowseHistoryMessage{

  private val circeEncoder: Encoder[AddBrowseHistoryMessage] = deriveEncoder
  private val circeDecoder: Decoder[AddBrowseHistoryMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[AddBrowseHistoryMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[AddBrowseHistoryMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[AddBrowseHistoryMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given addBrowseHistoryMessageEncoder: Encoder[AddBrowseHistoryMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given addBrowseHistoryMessageDecoder: Decoder[AddBrowseHistoryMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

}
