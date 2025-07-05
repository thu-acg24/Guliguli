package Impl


import Common.API.PlanContext
import Common.APIException.InvalidInputException
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import Objects.CommentService.Comment
import cats.effect.IO
import cats.implicits._
import io.circe.Json
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class QueryCommentByIDMessagePlanner(
    commentID: Int,
    override val planContext: PlanContext
) extends Planner[Comment] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Comment] = {
    for {
      // Step 1: Check if the commentID exists
      _ <- IO(logger.info(s"[QueryCommentByID] 校验 commentID=${commentID} 是否存在，并获取评论详细信息"))
      maybeComment <- fetchCommentDetails(commentID)
      comment <- maybeComment.liftTo[IO](InvalidInputException("commentID=${commentID} 不存在"))
      // Step 3: Wrap result and return
      _ <- IO(logger.info(s"[QueryCommentByID] 封装返回结果完成"))
    } yield comment
  }

  /**
   * 获取评论详细信息并构造成 Comment 对象
   * @param commentID 评论ID
   * @return Option[Comment]，如果存在返回评论对象；否则返回None
   */
  private def fetchCommentDetails(commentID: Int)(using PlanContext): IO[Option[Comment]] = {
    val sql =
      s"""
         |SELECT comment_id, content, video_id, author_id, reply_to_id, likes, time_stamp
         |FROM ${schemaName}.comment_table
         |WHERE comment_id = ?;
       """.stripMargin
    IO(logger.info(s"[QueryCommentByID] 获取 commentID=${commentID} 详细信息的SQL: ${sql}"))
    readDBJsonOptional(sql, List(SqlParameter("Int", commentID.toString))).map { jsonOpt =>
      jsonOpt.map { json =>
        Comment(
          commentID = decodeField[Int](json, "comment_id"),
          content = decodeField[String](json, "content"),
          videoID = decodeField[Int](json, "video_id"),
          authorID = decodeField[Int](json, "author_id"),
          replyToID = decodeField[Option[Int]](json, "reply_to_id"),
          likes = decodeField[Int](json, "likes"),
          timestamp = new DateTime(decodeField[Long](json, "time_stamp"))
        )
      }
    }
  }
}