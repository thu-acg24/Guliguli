package Impl


import APIs.UserService.GetUIDByTokenMessage
import Common.API.PlanContext
import Common.API.Planner
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.decodeDateTime
import Common.Serialize.CustomColumnTypes.encodeDateTime
import Common.ServiceUtils.schemaName
import cats.effect.IO
import cats.implicits.*
import cats.implicits._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory

case class ClearHistoryMessagePlanner(
                                       token: String,
                                       override val planContext: PlanContext
                                     ) extends Planner[Unit] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info(s"开始处理清空历史记录请求，用户Token为：${token}"))

      // 验证用户Token并获取对应的用户ID
      userID <- validateTokenAndFetchUserID(token)
      _ <- IO(logger.info(s"Token解析成功，用户ID为：${userID}"))
      clearResult <- clearUserHistory(userID)
      _ <- IO(logger.info(s"历史记录清空结果：${clearResult}"))
      // 根据用户ID进行相应的操作
    } yield ()
  }

  // 验证用户Token并获取用户ID
  private def validateTokenAndFetchUserID(token: String)(using PlanContext): IO[Int] = {
    for {
      _ <- IO(logger.info(s"调用 GetUIDByTokenMessage 根据用户Token获取用户ID"))
      userID <- GetUIDByTokenMessage(token).send
    } yield userID
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