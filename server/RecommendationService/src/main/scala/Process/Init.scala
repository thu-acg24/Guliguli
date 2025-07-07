
package Process


import Common.API.API
import Common.API.PlanContext
import Common.API.TraceID
import Common.DBAPI.{initSchema, readDBJsonOptional, writeDB}
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Global.DBConfig
import Global.GlobalVariables
import Global.ServerConfig
import Objects.PGVector.defaultDim
import Process.ProcessUtils.server2DB
import cats.effect.IO
import io.circe.generic.auto.*

import java.util.UUID

object Init {
  def init(config: ServerConfig): IO[Unit] = {
    given PlanContext = PlanContext(traceID = TraceID(UUID.randomUUID().toString), 0)
    given DBConfig = server2DB(config)

    val program: IO[Unit] = for {
      _ <- IO(GlobalVariables.isTest=config.isTest)
      _ <- API.init(config.maximumClientConnection)
      _ <- Common.DBAPI.SwitchDataSourceMessage(projectName = Global.ServiceCenter.projectName).send
      _ <- initSchema(schemaName)
      _ <- writeDB("CREATE EXTENSION IF NOT EXISTS VECTOR", List())
      /** 包含视频基础信息的表，支持RecommendationService的功能
       * video_id: 对应视频的唯一ID，主键
       * title: 视频标题
       * visible: 视频是否对大众可见
       * embedding: 视频对应向量
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."video_info_table" (
            video_id INT NOT NULL PRIMARY KEY,
            view_count INT NOT NULL DEFAULT 0,
            title TEXT NOT NULL,
            visible BOOLEAN DEFAULT TRUE,
            embedding VECTOR($defaultDim)
        );
        """,
        List()
      )
      /** 包含用户喜好信息的表，支持RecommendationService的功能
       * user_id: 对应用户的唯一ID，主键
       * embedding: 用户对应向量
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."user_info_table" (
            user_id INT NOT NULL PRIMARY KEY,
            embedding VECTOR($defaultDim)
        );
        """,
        List()
      )
      /** 记录用户观看视频的详细信息
       * watch_id: 用户观看记录的唯一ID
       * user_id: 观看用户的ID
       * video_id: 观看的视频ID
       * created_at: 记录添加时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."video_record_table" (
            watch_id SERIAL NOT NULL PRIMARY KEY,
            user_id INT NOT NULL,
            video_id INT NOT NULL,
            created_at TIMESTAMP NOT NULL
        );
        """,
        List()
      )
    } yield ()

    program.handleErrorWith(err => IO {
      println("[Error] Process.Init.init 失败, 请检查 db-manager 是否启动及端口问题")
      err.printStackTrace()
    })
  }
}
    