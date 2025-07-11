package Global

object ServiceCenter {
  val projectName: String = "Guliguli"
  val dbManagerServiceCode = "A000001"
  val tongWenDBServiceCode = "A000002"
  val tongWenServiceCode = "A000003"

  val CommentServiceCode = "A000010"
  val RecommendationServiceCode = "A000011"
  val UserServiceCode = "A000012"
  val MessageServiceCode = "A000013"
  val DanmakuServiceCode = "A000014"
  val ReportServiceCode = "A000015"
  val VideoServiceCode = "A000016"
  val HistoryServiceCode = "A000017"

  val fullNameMap: Map[String, String] = Map(
    tongWenDBServiceCode -> "DB-Manager（DB-Manager）",
    tongWenServiceCode -> "Tong-Wen（Tong-Wen）",
    CommentServiceCode -> "CommentService（CommentService)",
    RecommendationServiceCode -> "RecommendationService（RecommendationService)",
    UserServiceCode -> "UserService（UserService)",
    MessageServiceCode -> "MessageService（MessageService)",
    DanmakuServiceCode -> "DanmakuService（DanmakuService)",
    ReportServiceCode -> "ReportService（ReportService)",
    VideoServiceCode -> "VideoService（VideoService)",
    HistoryServiceCode -> "HistoryService（HistoryService)"
  )

  def serviceName(serviceCode: String): String = {
    fullNameMap(serviceCode).toLowerCase
  }
}
