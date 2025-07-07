package Impl


import APIs.UserService.GetUIDByTokenMessage
import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.PGVector
import Objects.VideoService.Video
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import cats.implicits.*
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.joda.time.{DateTime, Days}
import org.slf4j.LoggerFactory
import Utils.PerferenceProcess.updateEmbedding

case class RecordWatchDataMessagePlanner(
  token: String,
  videoID: Int,
  override val planContext: PlanContext
) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate token and get user ID
      userID <- GetUIDByTokenMessage(token).send
      _ <- QueryVideoInfoMessage(Some(token), videoID).send
      _ <- recordWatchData(userID, videoID)
    } yield ()
  }

  private def recordWatchData(userID: Int, videoID: Int)(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"开始记录观看数据: userID=$userID, videoID=$videoID"))
      _ <- IO(logger.info(s"开始查找用户的观看记录"))
      lastWatchOpt <- readDBJsonOptional(
        s"""
           |SELECT watch_id, created_at
           |FROM ${schemaName}.video_record_table
           |WHERE user_id = ? AND video_id = ?;
           |""".stripMargin, List(
          SqlParameter("Int", userID.toString),
          SqlParameter("Int", videoID.toString)
        ))
      createdAt <- IO(DateTime.now())
      _ <- lastWatchOpt match {
        case Some(json) => writeDB(
          s"""
             |UPDATE ${schemaName}.video_record_table
             |SET created_at = ?
             |WHERE watch_id = ?
             |""".stripMargin,List(
            SqlParameter("DateTime", createdAt.getMillis.toString),
            SqlParameter("Int", decodeField[Int](json, "watch_id").toString)
          )) >> IO(
            if (Days.daysBetween(decodeField[DateTime](json, "created_at"), createdAt).getDays >= 1) {
              updateEmbedding(userID, videoID, None, Some(0.05F))
              updateViewCount(videoID)
            }
        )
        case None => writeDB(
          s"""
             |INSERT INTO ${schemaName}.video_record_table
             |(user_id, video_id, created_at)
             |VALUES (?, ?, ?)
             |""".stripMargin,List(
            SqlParameter("Int", userID.toString),
            SqlParameter("Int", videoID.toString),
            SqlParameter("DateTime", createdAt.getMillis.toString)
          )) >>
          updateEmbedding(userID, videoID, Some(0.01F), Some(0.05F)) >>
          updateViewCount(videoID)
      }
    } yield()
  }
  private def updateViewCount(videoID: Int)(using PlanContext): IO[Unit] = {
    writeDB(
      s"""
         |UPDATE ${schemaName}.video_info_table
         |SET view_count = view_count + 1
         |WHERE video_id = ?
         |""".stripMargin, List(
        SqlParameter("Int", videoID.toString)
      )).void
  }
}