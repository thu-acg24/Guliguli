package Impl


import Utils.AuthProcess.validateToken
import Common.API.{PlanContext, Planner}
import Common.DBAPI.*
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.{Logger, LoggerFactory}
import org.joda.time.DateTime
import io.circe.Json
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import io.circe.*
import io.circe.syntax.*
import io.circe.generic.auto.*
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI.*
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Utils.AuthProcess.validateToken
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}

case class ChangeFollowStatusMessagePlanner(
                                             token: String,
                                             followeeID: Int,
                                             isFollow: Boolean,
                                             override val planContext: PlanContext
                                           ) extends Planner[Unit] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"Start processing with token=$token, followeeID=$followeeID, isFollow=$isFollow"))
      userID <- validateToken(token)
      _ <- checkUserExists(followeeID)
      _ <- if (isFollow) handleFollow(userID, followeeID) else handleUnfollow(userID, followeeID)
    } yield()
  }

  // 可以改成公共方法。
  private def checkUserExists(userID: Int)(using PlanContext): IO[Unit] = {
    val sql =
      s"""
    SELECT 1
    FROM $schemaName.user_table
    WHERE user_id = ?
           """.stripMargin

    readDBJsonOptional(sql, List(SqlParameter("Int", userID.toString)))
      .flatMap {
        case Some(_) => IO.unit // 查询到目标用户
        case None =>
          IO(logger.info(s"[ChangeFollowStatus] 未在数据库中找到目标用户(userID=$userID)")) *>
          IO.raiseError(new RuntimeException("未在数据库中找到目标用户"))
      }
      .handleErrorWith { ex =>
        IO(logger.error(s"[ChangeFollowStatus] 查询目标用户(userID=$userID)时发生错误: ${ex.getMessage}")) *>
        IO.raiseError(new RuntimeException(s"查询目标用户时发生错误: ${ex.getMessage}"))
      }
  }

  private def handleFollow(followerID: Int, followeeID: Int)(using PlanContext): IO[Unit] = {
    val checkSQL =
      s"""
         SELECT 1
         FROM $schemaName.follow_relation_table
         WHERE follower_id = ? AND followee_id = ?;
       """
    val insertSQL =
      s"""
         INSERT INTO $schemaName.follow_relation_table (follower_id, followee_id, timestamp)
         VALUES (?, ?, ?);
       """
    val params = List(SqlParameter("Int", followerID.toString), SqlParameter("Int", followeeID.toString))

    for {
      _ <- IO(logger.info(s"Handling follow action for followerID=$followerID and followeeID=$followeeID."))
      alreadyExists <- readDBBoolean(checkSQL, params).handleErrorWith { ex =>
          IO(logger.error(s"[ChangeFollowStatus] 查询关注关系时发生错误：${ex.getMessage}")) *>
          IO.raiseError(new RuntimeException(s"查询关注关系时发生错误：${ex.getMessage}"))
        }
      _ <- IO(if (alreadyExists) {
        logger.info(s"Follow relation already exists between followerID=$followerID and followeeID=$followeeID.")
      } else {
        val timestamp = DateTime.now().getMillis.toString
        IO(logger.info(s"Inserting new follow relation for followerID=$followerID and followeeID=$followeeID.")) *>
        writeDB(insertSQL, params :+ SqlParameter("DateTime", timestamp))
          .handleErrorWith { ex =>
            IO(logger.error(s"[ChangeFollowStatus] 将关注关系插入数据库时发生错误：${ex.getMessage}")) *>
            IO.raiseError(new RuntimeException(s"将关注关系插入数据库时发生错误：${ex.getMessage}"))
          }
      })
    } yield()
  }

  private def handleUnfollow(followerID: Int, followeeID: Int)(using PlanContext): IO[Unit] = {

    val checkSQL =
      s"""
         SELECT 1
         FROM $schemaName.follow_relation_table
         WHERE follower_id = ? AND followee_id = ?;
       """
    val deleteSQL =
      s"""
         DELETE FROM $schemaName.follow_relation_table
         WHERE follower_id = ? AND followee_id = ?;
       """
    val params = List(SqlParameter("Int", followerID.toString), SqlParameter("Int", followeeID.toString))

    for {
      _ <- IO(logger.info(s"Handling unfollow action for followerID=$followerID and followeeID=$followeeID."))
      exists <- readDBBoolean(checkSQL, params).handleErrorWith { ex =>
        logger.error(s"[ChangeFollowStatus] 查询关注关系时发生错误：${ex.getMessage}")
        throw RuntimeException(s"查询关注关系时发生错误：${ex.getMessage}")
      }
      _ <- if (!exists) {
          IO(logger.info(s"No follow relation exists between followerID=$followerID and followeeID=$followeeID."))
        } else {
          IO(logger.info(s"Deleting follow relation between followerID=$followerID and followeeID=$followeeID.")) >>
          writeDB(deleteSQL, params)
            .handleErrorWith { ex =>
              IO(logger.error(s"[ChangeFollowStatus] 删除关注关系时发生错误：${ex.getMessage}")) *>
              IO.raiseError(RuntimeException(s"删除关注关系时发生错误：${ex.getMessage}"))
            }
        }
    } yield ()
  }
}