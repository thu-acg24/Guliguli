package Utils

import Common.API.{PlanContext, Planner}
import Common.APIException.InvalidInputException
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import cats.effect.IO
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import Objects.VideoService.{Video, VideoStatus}

//process plan import 预留标志位，不要删除

case object DecodeVideo {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除

  // implicit val dateTimeDecoder: Decoder[DateTime] = decodeDateTime

  def decodeVideo(json: Json)(using PlanContext): Video = {
    val updatedJson = json.mapObject { obj =>
      obj("tag").flatMap(_.asString) match {
        case Some(tagStr) =>
          // 预处理字符串格式
          val jsonStr = tagStr.replace('{', '[').replace('}', ']')

          // 解析为JSON值
          val parsedJson = parser.parse(jsonStr).getOrElse(Json.Null)
          obj.add("tag", parsedJson)

        case None => obj
      }
    }
    //.mapObject { obj =>
    //  obj("upload_time").flatMap(_.asString) match {
    //    case Some(timeStr) =>
    //      obj.add("tag", encodeDateTime(DateTime(timeStr.toLong)))
    //
    //    case None => obj
    //  }
    //}
    logger.info(s"Updated json: $updatedJson")
    decodeType[Video](updatedJson)
  }
}