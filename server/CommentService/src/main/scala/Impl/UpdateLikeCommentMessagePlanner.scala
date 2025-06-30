package Impl


import Objects.CommentService.Comment
import APIs.CommentService.QueryCommentByIDMessage
import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}

case class UpdateLikeCommentMessagePlanner(
    token: String,
    commentID: Int,
    isLike: Boolean,
    override val planContext: PlanContext
) extends Planner[Unit] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Unit] = {
    for {
      // Step 1: Validate token and retrieve userID
      _ <- IO(logger.info(s"[Step 1] Validating token: ${token}"))
      userID <- GetUIDByTokenMessage(token).send

      // Step 2: Validate comment existence
      _ <- IO(logger.info(s"[Step 2] Validating existence of comment with ID: ${commentID}"))
      comment <- QueryCommentByIDMessage(commentID).send
      // Step 3: Determine if like or unlike operation
      _ <- if isLike then performLikeOperation(userID, comment) else performUnlikeOperation(userID, comment)
      // Step 4: Log success and return None
      _ <- IO(logger.info(s"[Step 4] Successfully updated like status for commentID=${commentID}"))
    } yield ()
  }

  private def performLikeOperation(userID: Int, comment: Comment)(using PlanContext): IO[Unit] = {
    val sqlCheckRecord = s"SELECT * FROM ${schemaName}.like_comment_record_table WHERE user_id = ? AND comment_id = ?;"
    for {
      // Step 3.1.1: Check if the user already liked the comment
      _ <- IO(logger.info(s"[Perform Like] Checking if userID=${userID} already liked commentID=${comment.commentID}"))
      likeRecordOption <- readDBJsonOptional(sqlCheckRecord, List(
        SqlParameter("Int", userID.toString),
        SqlParameter("Int", comment.commentID.toString)
      ))
      _ <- likeRecordOption match {
        case Some(_) =>
          IO(logger.warn(s"[Perform Like] UserID ${userID} has already liked CommentID ${comment.commentID}")) *>
            IO.raiseError(new IllegalStateException("Already Liked"))
        case None =>
          for {
            // Increment likes in CommentTable
            _ <- IO(logger.info(s"[Perform Like] Incrementing like count for CommentID ${comment.commentID}"))
            updateLikesSql = s"UPDATE ${schemaName}.comment_table SET likes = likes + 1 WHERE comment_id = ?;"
            _ <- writeDB(updateLikesSql, List(SqlParameter("Int", comment.commentID.toString)))

            // Add record to LikeCommentRecordTable
            _ <- IO(logger.info(s"[Perform Like] Adding like record for userID=${userID} and commentID=${comment.commentID}"))
            insertRecordSql = s"""
              INSERT INTO ${schemaName}.like_comment_record_table (user_id, comment_id, timestamp)
              VALUES (?, ?, ?);
            """
            _ <- writeDB(insertRecordSql, List(
              SqlParameter("Int", userID.toString),
              SqlParameter("Int", comment.commentID.toString),
              SqlParameter("DateTime", DateTime.now.getMillis.toString)
            ))
          } yield ()
      }
    } yield ()
  }

  private def performUnlikeOperation(userID: Int, comment: Comment)(using PlanContext): IO[Unit] = {
    val sqlCheckRecord = s"SELECT * FROM ${schemaName}.like_comment_record_table WHERE user_id = ? AND comment_id = ?;"
    for {
      // Step 3.2.1: Check if the user has liked the comment
      _ <- IO(logger.info(s"[Perform Unlike] Checking if userID=${userID} has liked commentID=${comment.commentID}"))
      likeRecordOption <- readDBJsonOptional(sqlCheckRecord, List(
        SqlParameter("Int", userID.toString),
        SqlParameter("Int", comment.commentID.toString)
      ))
      _ <- likeRecordOption match {
        case None =>
          IO(logger.warn(s"[Perform Unlike] UserID ${userID} hasn't liked CommentID ${comment.commentID} yet")) *>
            IO.raiseError(new IllegalStateException("Not Liked Yet"))
        case Some(_) =>
          for {
            // Decrement likes in CommentTable
            _ <- IO(logger.info(s"[Perform Unlike] Decrementing like count for CommentID ${comment.commentID}"))
            updateLikesSql = s"UPDATE ${schemaName}.comment_table SET likes = likes - 1 WHERE comment_id = ?;"
            _ <- writeDB(updateLikesSql, List(SqlParameter("Int", comment.commentID.toString)))

            // Remove record from LikeCommentRecordTable
            _ <- IO(logger.info(s"[Perform Unlike] Removing like record for userID=${userID} and commentID=${comment.commentID}"))
            deleteRecordSql = s"DELETE FROM ${schemaName}.like_comment_record_table WHERE user_id = ? AND comment_id = ?;"
            _ <- writeDB(deleteRecordSql, List(
              SqlParameter("Int", userID.toString),
              SqlParameter("Int", comment.commentID.toString)
            ))
          } yield ()
      }
    } yield ()
  }
}