package Global

object ServiceCenter {
  val projectName: String = "DB-Manager"

  val tongWenDBServiceCode = "A000002"
  val tongWenServiceCode = "A000003"

  val fullNameMap: Map[String, String] = Map(
    tongWenDBServiceCode -> "DB-Manager（DB-Manager）",
    tongWenServiceCode -> "Tong-Wen（Tong-Wen）",
  )
}
