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
import Objects.UserService.FollowRelation

/**
 * QueryFollowingListMessage
 * desc: 查询某用户的关注列表，返回他们关注的用户信息。
 * @param userID: Int (当前需要查询关注列表的目标用户ID。)
 * @param rangeL: Int (关注列表数据中提取的起始位置索引。)
 * @param rangeR: Int (关注列表数据中提取的结束位置索引。)
 * @return followList: FollowRelation:1181 (目标用户的关注记录列表。)
 */

case class QueryFollowingListMessage(
  userID: Int,
  rangeL: Int,
  rangeR: Int
) extends API[List[FollowRelation]](UserServiceCode)



case object QueryFollowingListMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryFollowingListMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryFollowingListMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryFollowingListMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryFollowingListMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryFollowingListMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryFollowingListMessageEncoder: Encoder[QueryFollowingListMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryFollowingListMessageDecoder: Decoder[QueryFollowingListMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

