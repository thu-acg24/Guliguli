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
                                           ) extends Planner[Option[String]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    IO(logger.info(s"Start processing with token=$token, followeeID=$followeeID, isFollow=$isFollow")) >>
    // 我们的接口定义形式注定无法使用简单的 for comprehension（需要使用EitherT之类的东西，还不如flatMap嵌套），因为返回None的场景和一般场景不同。
    // 可以提前return但是IO返回值不支持！！！！废物啊！！！
    validateToken(token).flatMap {
      case Some(userId) =>
        checkUserExists(followeeID).flatMap {
          case None =>
            if (isFollow) handleFollow(userId, followeeID)
            else handleUnfollow(userId, followeeID)
          case result =>
            IO.pure(result)
        }
      case None => IO.pure(Some("Invalid Token"))
    }
  }

  // 可以改成公共方法。
  def checkUserExists(userID: Int)(using PlanContext): IO[Option[String]] = {
    val sql =
      s"""
    SELECT 1
    FROM ${schemaName}.user_table
    WHERE user_id = ?
           """.stripMargin

    readDBJsonOptional(sql, List(SqlParameter("Int", userID.toString)))
      .flatMap {
        case Some(_) => IO.pure(None) // 查询到目标用户
        case None =>
          IO(logger.info(s"[ChangeFollowStatus] 未在数据库中找到目标用户(userID=${userID})")) >>
            IO.pure(Some("目标用户不存在"))
      }
      .handleErrorWith { ex =>
        IO(logger.error(s"[ChangeFollowStatus] 查询目标用户(userID=${userID})时发生错误: ${ex.getMessage}")) >>
          IO.pure(Some("查询目标用户时发生错误"))
      }
  }

  private def handleFollow(followerID: Int, followeeID: Int)(using PlanContext): IO[Option[String]] = {
    logger.info(s"Handling follow action for followerID=${followerID} and followeeID=${followeeID}.")

    val checkSQL =
      s"""
         SELECT 1
         FROM ${schemaName}.follow_relation_table
         WHERE follower_id = ? AND followee_id = ?;
       """
    val insertSQL =
      s"""
         INSERT INTO ${schemaName}.follow_relation_table (follower_id, followee_id, timestamp)
         VALUES (?, ?, ?);
       """
    val params = List(SqlParameter("Int", followerID.toString), SqlParameter("Int", followeeID.toString))

    for {
      alreadyExists <- readDBBoolean(checkSQL, params)
      result <- if (alreadyExists) {
        logger.info(s"Follow relation already exists between followerID=${followerID} and followeeID=${followeeID}.")
        IO.pure(Some("Already following the user"))
      } else {
        logger.info(s"Inserting new follow relation for followerID=${followerID} and followeeID=${followeeID}.")
        val timestamp = DateTime.now().getMillis.toString
        writeDB(insertSQL, params :+ SqlParameter("DateTime", timestamp)).map(_ => None)
      }
    } yield result
  }

  private def handleUnfollow(followerID: Int, followeeID: Int)(using PlanContext): IO[Option[String]] = {
    logger.info(s"Handling unfollow action for followerID=${followerID} and followeeID=${followeeID}.")

    val checkSQL =
      s"""
         SELECT 1
         FROM ${schemaName}.follow_relation_table
         WHERE follower_id = ? AND followee_id = ?;
       """
    val deleteSQL =
      s"""
         DELETE FROM ${schemaName}.follow_relation_table
         WHERE follower_id = ? AND followee_id = ?;
       """
    val params = List(SqlParameter("Int", followerID.toString), SqlParameter("Int", followeeID.toString))

    for {
      exists <- readDBBoolean(checkSQL, params)
      result <- if (!exists) {
        logger.info(s"No follow relation exists between followerID=${followerID} and followeeID=${followeeID}.")
        IO.pure(Some("Not following the user"))
      } else {
        logger.info(s"Deleting follow relation between followerID=${followerID} and followeeID=${followeeID}.")
        writeDB(deleteSQL, params).map(_ => None)
      }
    } yield result
  }
}