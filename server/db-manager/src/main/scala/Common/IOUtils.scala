package Common

import cats.effect.IO
import io.circe.{Decoder, DecodingFailure, Encoder, Json, JsonObject}
import Common.Serialize.CustomColumnTypes.*

object IOUtils {
  def addTypeField(json: Json, typeName: String): Json =
    json.asObject match {
      case Some(jsonObject) => // If the json can be converted to a JsonObject
        Json.fromJsonObject(jsonObject.add("type", Json.fromString(typeName)))
      case None => 
        json 
    }
  def raiseError(st:String): IO[String]=
    throw new Exception(st)
    
  def assertIO(assertion: Boolean, message: String): IO[Unit] =
    if !assertion then throw Exception(message)
    else IO.unit
}
