package Common.Object


import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.generic.semiauto.deriveEncoder

case class SqlParameter(dataType: String, value: String)

object SqlParameter {
  // Encoder for SqlParam
  implicit val encodeSqlParameter: Encoder[SqlParameter] = deriveEncoder[SqlParameter]

  // Decoder for SqlParam
  implicit val decodeSqlParameter: Decoder[SqlParameter] = new Decoder[SqlParameter] {
    final def apply(c: HCursor): Decoder.Result[SqlParameter] = for {
      dataType <- c.downField("dataType").as[String]
      value <- c.downField("value").as[String]
    } yield {
      dataType.toLowerCase match {
        case "string" => SqlParameter("String", value)
        case "int" => SqlParameter("Int", value)
        case "boolean" => SqlParameter("Boolean", value)
        case "long" => SqlParameter("Long", value)
        case "datetime" => SqlParameter("DateTime", value) // Add case for DateTime
        case "array[int]" => SqlParameter("Array[Int]", value) // Add case for Array[Int]
        case "array[string]" => SqlParameter("Array[String]", value) // Add case for Array[Int]
        case "vector" => SqlParameter("Vector", value) // Add case for Vector
        // Add more type cases as needed
        case s => throw new Exception(s"Unsupported data type ${s}")
      }
    }
  }
}