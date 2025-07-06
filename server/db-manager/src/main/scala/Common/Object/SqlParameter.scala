package Common.Object

import Common.Serialize.JacksonSerializeUtils
import com.fasterxml.jackson.core.`type`.TypeReference
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Decoder, Encoder, HCursor}

// Define the SqlParam case class
case class SqlParameter(dataType: String, value: String)

object SqlParameter {
  // Encoder for SqlParam
  implicit val encodeSqlParameter: Encoder[SqlParameter] = deriveEncoder[SqlParameter]

  // Decoder for SqlParam - Circe Decoder，
  // 借用 Jackson 解析 JSON 后应用 `dataType` 转换 => 主要是为了支持反序列化 String is null
  implicit val decodeSqlParameter: Decoder[SqlParameter] = Decoder.instance { (c: HCursor) =>
    // 先用 Jackson 解析 JSON
    val decoded = try {
      Right(JacksonSerializeUtils.deserialize(c.value.noSpaces, new TypeReference[SqlParameter]{}))
    } catch {
      case e: Exception => Left(io.circe.DecodingFailure(e.getMessage, c.history))
    }

    // 解析成功后，再应用 dataType 逻辑
    decoded.map { param =>
      normalizeDataType(param.dataType, param.value)
    }
  }

  private def normalizeDataType(dataType: String, value: String): SqlParameter = {
    dataType.toLowerCase match {
      case "string"       => SqlParameter("String", value)
      case "object" => SqlParameter("Object", value)
      case "int"          => SqlParameter("Int", value)
      case "boolean"      => SqlParameter("Boolean", value)
      case "long"         => SqlParameter("Long", value)
      case "vector"         => SqlParameter("Vector", value)
      case "bigint"       => SqlParameter("Long", value)
      case "double"       => SqlParameter("Double", value)
      case "float"        => SqlParameter("Float", value)
      case "datetime"     => SqlParameter("DateTime", value)
      case "array[int]"   => SqlParameter("Array[Int]", value)
      case "array[string]"=> SqlParameter("Array[String]", value)
      case "array[object]" => SqlParameter("Array[Object]", value)
      case "array[long]"  => SqlParameter("Array[Long]", value)
      case s              => throw new Exception(s"Unsupported data type ${s}")
    }
  }




}