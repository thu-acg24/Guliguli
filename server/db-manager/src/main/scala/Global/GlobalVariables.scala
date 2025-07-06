package Global

import Common.Object.SqlParameter
import Common.Serialize.JacksonSerializeUtils
import Global.ServiceCenter.*
import com.fasterxml.jackson.core.`type`.TypeReference


object GlobalVariables {
  lazy val serviceCode : String = tongWenDBServiceCode

  val projectIDLength:Int=20

  val openAIKey = ""
}
