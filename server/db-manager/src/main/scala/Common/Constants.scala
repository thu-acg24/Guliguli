package Common

import scala.util.Try


object Constants {
  val masterBranch: String = "master"

  val basicScalaTypes: List[String] = List("String", "Boolean", "Int", "Long", "Float", "Double", "Any")
  val basicJavaTypes: List[String] = List("String", "Boolean", "Integer", "Long", "Float", "Double", "Int", "MultipartFile", "Decimal")
  val basicTsTypes: List[String] = List("string", "number", "boolean")
  val basicTypes: List[String] = (basicScalaTypes ++ basicJavaTypes ++ basicTsTypes).distinct

  val scalaTimeImportStr: String = "import org.joda.time.DateTime"
  val scalaTimeType: String = "DateTime"
  val javaTimeType: String = "Date"
  val tsTimeType: String = "number"
  val parameterTimeType: String = "TimeStamp"
  val scalaTimeTypeSet: Seq[String] = List(scalaTimeType, parameterTimeType)
  
  val javaUUIDImportStr: String = "import java.util.UUID"
  val javaUUIDType: String = "UUID"
  val stringTsType = "string"
  
  val scalaVoidType = "Unit"
  val scalaVoidReturnVal = "()"
  
  // scala 中不是 shared 类型的所有类型定义
  lazy val scalaNotSharedTypeSet: Set[String] = (basicScalaTypes ++ scalaTimeTypeSet.toList ++ List(javaUUIDType)).toSet


  // 关键字不能用在 paramName

  lazy val scalaMissingParamName: Set[String] = Set(
    "enum", "type", "case", "class"
  )


  val backendCodeFileSuffix: Set[String] = Set(
    ".scala",
    ".java",
    ".xml"
  )

  lazy val allowCodeFileSuffix: Set[String] = Set(
    ".scala",
    ".java",
    ".xml",
    ".properties",
    ".yaml",
    ".json"
  ) ++ Try {
    System.getProperty("CodeFileSuffixExtensions").split(",").toSet
  }.getOrElse(Set.empty) // 系统变量拓展


  val postgresMissingParamNames: Set[String] = Set(
    "all",
    "analyse",
    "analyze",
    "and",
    "any",
    "array",
    "as",
    "asc",
    "asymmetric",
    "both",
    "case",
    "cast",
    "check",
    "collate",
    "column",
    "constraint",
    "create",
    "current_catalog",
    "current_date",
    "current_role",
    "current_time",
    "current_timestamp",
    "current_user",
    "default",
    "deferrable",
    "desc",
    "distinct",
    "do",
    "else",
    "end",
    "except",
    "false",
    "fetch",
    "for",
    "foreign",
    "from",
    "grant",
    "group",
    "having",
    "in",
    "initially",
    "intersect",
    "into",
    "lateral",
    "leading",
    "limit",
    "localtime",
    "localtimestamp",
    "not",
    "null",
    "offset",
    "on",
    "only",
    "or",
    "order",
    "placing",
    "primary",
    "references",
    "returning",
    "select",
    "session_user",
    "some",
    "symmetric",
    "system_user",
    "table",
    "then",
    "to",
    "trailing",
    "true",
    "union",
    "unique",
    "user",
    "using",
    "variadic",
    "when",
    "where",
    "window",
    "with"
  )


  lazy val missingParamNames: Set[String] = scalaMissingParamName ++ postgresMissingParamNames

}

