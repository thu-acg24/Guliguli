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
 * QueryFollowerListMessage
 * desc: 查询某用户的粉丝列表，返回关注关系中的记录。返回按照关注时间第rangeL条到第rangeR条，均包含。
 * @param userID: Int (目标用户的唯一标识。)
 * @param rangeL: Int (要查询的粉丝列表的起始范围（包含）。)
 * @param rangeR: Int (要查询的粉丝列表的结束范围（包含）。)
 * @return followerList: FollowRelation:1181 (包含粉丝列表的关注关系列表，每个关系包括粉丝的相关信息。)
 */

case class QueryFollowerListMessage(
  userID: Int,
  rangeL: Int,
  rangeR: Int
) extends API[List[FollowRelation]](UserServiceCode)



case object QueryFollowerListMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryFollowerListMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryFollowerListMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryFollowerListMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryFollowerListMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryFollowerListMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryFollowerListMessageEncoder: Encoder[QueryFollowerListMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryFollowerListMessageDecoder: Decoder[QueryFollowerListMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

