package APIs.HistoryService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.HistoryServiceCode
import Objects.HistoryService.HistoryRecord
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
 * QueryHistoryMessage
 * desc: 根据用户Token校验后，查询用户的观看历史记录，返回按照 (timestamp, ID) 开始降序排序至多 fetchLimit 条记录
 * @param token: String (用户身份令牌，用于校验身份)
 * @param lastTime: DateTime (上一次查询到历史记录的浏览时间)
 * @param lastID: Int (上一次历史记录的ID)
 * @param fetchLimit: Int (至多获取多少条记录，不超过 100)
 * @return history: List[HistoryRecord] (返回用户观看历史记录的列表)
 */

case class QueryHistoryMessage(
  token: String,
  lastTime: DateTime,
  lastID: Int,
  fetchLimit: Int
) extends API[List[HistoryRecord]](HistoryServiceCode)

case object QueryHistoryMessage{

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
