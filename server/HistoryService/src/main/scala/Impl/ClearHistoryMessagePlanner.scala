package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName
import APIs.UserService.GetUIDByTokenMessage
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class ClearHistoryMessagePlanner(
                                       token: String,
                                       override val planContext: PlanContext
                                     ) extends Planner[Option[String]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Option[String]] = {
    for {
      _ <- IO(logger.info(s"开始处理清空历史记录请求，用户Token为：${token}"))

      // 验证用户Token并获取对应的用户ID
      userIDOpt <- validateTokenAndFetchUserID(token)

      // 根据用户ID进行相应的操作
      resultOpt <- userIDOpt match {
        case Some(userID) =>
          for {
            _ <- IO(logger.info(s"Token解析成功，用户ID为：${userID}"))

            // 清空观看历史记录
            clearResult <- clearUserHistory(userID)

            _ <- IO(logger.info(s"历史记录清空结果：${clearResult}"))
          } yield None // 表示操作成功，返回None
        case None =>
          IO {
            logger.error("Token解析失败，无法获取用户ID")
            Some("Invalid token") // Token无效，返回错误信息
          }
      }
    } yield resultOpt
  }

  // 验证用户Token并获取用户ID
  private def validateTokenAndFetchUserID(token: String)(using PlanContext): IO[Option[Int]] = {
    for {
      _ <- IO(logger.info(s"调用 GetUIDByTokenMessage 根据用户Token获取用户ID"))
      userIDOpt <- GetUIDByTokenMessage(token).send
    } yield userIDOpt
  }

  // 删除用户的观看历史记录
  private def clearUserHistory(userID: Int)(using PlanContext): IO[String] = {
    for {
      _ <- IO(logger.info(s"准备清空用户[${userID}]的观看历史记录"))
      sql <- IO {
        s"""
           DELETE FROM ${schemaName}.history_record_table
           WHERE user_id = ?;
         """
      }
      _ <- IO(logger.info(s"执行清空历史记录的SQL指令：${sql}"))
      result <- writeDB(sql, List(SqlParameter("Int", userID.toString)))
    } yield result
  }
}