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
import Objects.HistoryService.HistoryRecord

/**
 * QueryHistoryMessage
 * desc: 根据用户Token校验后，查询用户的观看历史记录，返回从新到旧第rangeL条到第rangeR条，均包含。
 * @param token: String (用户身份令牌，用于校验身份)
 * @param rangeL: Int (分页查询的起始位置)
 * @param rangeR: Int (分页查询的结束位置)
 * @return history: HistoryRecord:1184 (返回用户观看历史记录的列表)
 */

case class QueryHistoryMessage(
  token: String,
  rangeL: Int,
  rangeR: Int
) extends API[Option[List[HistoryRecord]]](HistoryServiceCode)



case object QueryHistoryMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryHistoryMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryHistoryMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryHistoryMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryHistoryMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryHistoryMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryHistoryMessageEncoder: Encoder[QueryHistoryMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryHistoryMessageDecoder: Decoder[QueryHistoryMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

