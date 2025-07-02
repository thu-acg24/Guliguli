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
                                              videoId: Int,
                                              rangeL: Int,
                                              rangeR: Int,
                                              override val planContext: PlanContext
                                            ) extends Planner[List[Comment]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[Comment]] = {
    for {
      // Step 1: Validate videoId
      _ <- IO(logger.info(s"Validating videoId: ${videoId}"))
      _ <- validateVideoExists(videoId)

      // Step 2: Query paginated comments directly from CommentTable using SQL with LIMIT and OFFSET
      _ <- IO(logger.info(s"Querying paginated comments for videoId=${videoId}, rangeL=${rangeL}, rangeR=${rangeR}"))
      comments <- queryPaginatedComments(videoId, rangeL, rangeR)

      _ <- IO(logger.info(s"Query completed. Returning ${comments.size} comments"))
    } yield comments
  }

  /**
   * Validates if the given videoId exists in the database by calling `QueryVideoInfoMessage`.
   *
   * @param videoId Video ID to validate.
   * @return IO[Unit], raises an error if the video does not exist.
   */
  private def validateVideoExists(videoId: Int)(using PlanContext): IO[Unit] = {
    QueryVideoInfoMessage(None, videoId).send.flatMap { video =>
        IO(logger.info(s"Video with videoId=${videoId} exists. Title: '${video.title}'")) >> IO.unit
    }
  }

  /**
   * Queries the database for paginated comments associated with the given videoId.
   *
   * @param videoId Video ID to query comments for.
   * @param rangeL  Starting index (inclusive).
   * @param rangeR  Ending index (exclusive).
   * @return List of Comment objects.
   */
  private def queryPaginatedComments(videoId: Int, rangeL: Int, rangeR: Int)(using PlanContext): IO[List[Comment]] = {
    for {
      sql <- IO {
        s"""
           |SELECT comment_id, content, video_id, author_id, reply_to_id, likes, time_stamp
           |FROM ${schemaName}.comment_table
           |WHERE video_id = ?
           |ORDER BY time_stamp DESC
           |LIMIT ? OFFSET ?;
       """.stripMargin
      }
      // Calculate LIMIT and OFFSET based on provided range values
      limit <- IO.pure(rangeR - rangeL)
      offset <- IO.pure(rangeL)

      rows <- readDBRows(
        sql,
        List(
        SqlParameter("Int", videoId.toString),
        SqlParameter("Int", limit.toString),
        SqlParameter("Int", offset.toString)
        )
      )
      result <- IO(rows.map { json =>
      Comment (
        commentID = decodeField[Int] (json, "comment_id"),
        content = decodeField[String] (json, "content"),
        videoID = decodeField[Int] (json, "video_id"),
        authorID = decodeField[Int] (json, "author_id"),
        replyToID = decodeField[Option[Int]] (json, "reply_to_id"),
        likes = decodeField[Int] (json, "likes"),
        timestamp = decodeField[DateTime] (json, "time_stamp")
      )
    })
    } yield result
  }
}