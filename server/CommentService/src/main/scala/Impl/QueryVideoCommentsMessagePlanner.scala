package Impl


import APIs.VideoService.QueryVideoInfoMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.CommentService.Comment
import Objects.VideoService.VideoStatus
import cats.effect.IO
import cats.implicits.*
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryVideoCommentsMessagePlanner(
                                              videoID: Int,
                                              lastTime: DateTime,
                                              lastID: Int,
                                              rootID: Option[Int],
                                              fetchLimit: Int = 10,
                                              override val planContext: PlanContext
                                            ) extends Planner[List[Comment]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[Comment]] = {
    for {
      // Step 1: Validate videoID
      _ <- IO(logger.info(s"Validating videoID: ${videoID}"))
      _ <- validateVideoExists(videoID)

      // Step 2: Query paginated comments directly from CommentTable using SQL
      _ <- IO(logger.info(s"Querying paginated comments for videoID=${videoID}"))
      rows <- rootID match {
        case None => queryRootPaginatedComments()
        case Some(actualRootID) => queryPaginatedComments(actualRootID)
      }
      comments <- IO(rows.map { json =>
        Comment (
          commentID = decodeField[Int] (json, "comment_id"),
          content = decodeField[String] (json, "content"),
          videoID = decodeField[Int] (json, "video_id"),
          authorID = decodeField[Int] (json, "author_id"),
          replyToID = decodeField[Option[Int]] (json, "reply_to_id"),
          replyToUserID = decodeField[Option[Int]] (json, "reply_to_user_id"),
          likes = decodeField[Int] (json, "likes"),
          replyCount = decodeField[Int] (json, "reply_count"),
          timestamp = decodeField[DateTime] (json, "time_stamp")
        )
      })

      _ <- IO(logger.info(s"Query completed. Returning ${comments.size} comments"))
    } yield comments
  }

  /**
   * Validates if the given videoID exists in the database by calling `QueryVideoInfoMessage`.
   *
   * @param videoID Video ID to validate.
   * @return IO[Unit], raises an error if the video does not exist.
   */
  private def validateVideoExists(videoID: Int)(using PlanContext): IO[Unit] = {
    QueryVideoInfoMessage(None, videoID).send.flatMap { video =>
        IO(logger.info(s"Video with videoID=${videoID} exists. Title: '${video.title}'")) >> IO.unit
    }
  }

  /**
   * Queries the database for paginated comments associated with the given videoID.
   * @return List of Comment objects.
   */
  private def queryRootPaginatedComments()(using PlanContext): IO[List[Json]] = {
    for {
      sql <- IO {
        s"""
           |SELECT comment_id, content, video_id, author_id, reply_to_id, likes, reply_count, time_stamp
           |FROM ${schemaName}.comment_table
           |WHERE video_id = ? AND root_id IS NULL AND (time_stamp < ? OR (time_stamp = ? AND comment_id < ?))
           |ORDER BY time_stamp DESC, comment_id DESC LIMIT ?
       """.stripMargin
      }

      rows <- readDBRows(
        sql,
        List(
          SqlParameter("Int", videoID.toString),
          SqlParameter("DateTime", lastTime.getMillis.toString),
          SqlParameter("DateTime", lastTime.getMillis.toString),
          SqlParameter("Int", lastID.toString),
          SqlParameter("Int", fetchLimit.toString)
        )
      )
    } yield rows
  }

  /**
   * Queries the database for paginated comments associated with the given videoID and given rootID.
   * @return List of Comment objects.
   */
  private def queryPaginatedComments(rootID: Int)(using PlanContext): IO[List[Json]] = {
    for {
      sql <- IO {
        s"""
           |SELECT comment_id, content, video_id, author_id, reply_to_id, likes, reply_count, time_stamp
           |FROM ${schemaName}.comment_table
           |WHERE video_id = ? AND root_id = ? AND (time_stamp > ? OR (time_stamp = ? AND comment_id > ?))
           |ORDER BY time_stamp ASC, comment_id ASC LIMIT 20
       """.stripMargin
      }

      rows <- readDBRows(
        sql,
        List(
          SqlParameter("Int", videoID.toString),
          SqlParameter("Int", rootID.toString),
          SqlParameter("DateTime", lastTime.getMillis.toString),
          SqlParameter("DateTime", lastTime.getMillis.toString),
          SqlParameter("Int", lastID.toString)
        )
      )
    } yield rows
  }
}