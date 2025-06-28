package APIs.HistoryService

import Common.API.API
import Global.ServiceCenter.HistoryServiceCode

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
 * AddBrowseHistoryMessage
 * desc: 根据用户Token校验后，记录用户浏览的视频到历史记录表。
 * @param token: String (用户的身份验证信息，用于校验用户身份是否合法。)
 * @param videoID: Int (视频的唯一标识符，用于指定用户浏览的视频。)
 * @return result: String (操作结果的描述，记录是否成功或失败的信息。)
 */

case class AddBrowseHistoryMessage(
  token: String,
  videoID: Int
) extends API[Option[String]](HistoryServiceCode)



case object AddBrowseHistoryMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
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

