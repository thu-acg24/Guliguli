package Objects.DanmakuService


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
 * Danmaku
 * desc: 弹幕信息
 * @param danmakuID: Int (弹幕的唯一标识ID)
 * @param content: String (弹幕的具体内容)
 * @param videoID: Int (弹幕所属视频的唯一标识ID)
 * @param authorID: Int (发布弹幕的用户的唯一标识ID)
 * @param danmakuColor: String (弹幕的颜色值)
 * @param timeInVideo: Float (弹幕出现在视频中的时间点（秒）)
 */

case class Danmaku(
  danmakuID: Int,
  content: String,
  videoID: Int,
  authorID: Int,
  danmakuColor: String,
  timeInVideo: Float
){

  //process class code 预留标志位，不要删除


}


case object Danmaku{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[Danmaku] = deriveEncoder
  private val circeDecoder: Decoder[Danmaku] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[Danmaku] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[Danmaku] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[Danmaku]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given danmakuEncoder: Encoder[Danmaku] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given danmakuDecoder: Decoder[Danmaku] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

