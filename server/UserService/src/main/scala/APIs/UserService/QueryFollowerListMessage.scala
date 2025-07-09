package APIs.UserService


import Common.API.API
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.UserServiceCode
import Objects.UserService.FollowRelation
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
 * QueryFollowerListMessage
 * desc: 查询某用户的粉丝列表，返回关注关系中的记录。返回按照关注时间第rangeL条到第rangeR条，左闭右开。
 * @param userID: Int (目标用户的唯一标识。)
 * @param rangeL: Int (要查询的粉丝列表的起始范围。)
 * @param rangeR: Int (要查询的粉丝列表的结束范围。)
 * @return followerList: FollowRelation:1181 (包含粉丝列表的关注关系列表，每个关系包括粉丝的相关信息。)
 */

case class QueryFollowerListMessage(
  userID: Int,
  rangeL: Int,
  rangeR: Int
) extends API[List[FollowRelation]](UserServiceCode)

case object QueryFollowerListMessage{

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
