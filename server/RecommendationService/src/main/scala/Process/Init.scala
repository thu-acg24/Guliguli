
package Process


import Common.API.API
import Common.API.PlanContext
import Common.API.TraceID
import Common.DBAPI.initSchema
import Common.DBAPI.writeDB
import Common.ServiceUtils.schemaName
import Global.DBConfig
import Global.GlobalVariables
import Global.ServerConfig
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
      _ <- writeDB("CREATE EXTENSION VECTOR", List())
            /** 包含视频基础信息的表，支持RecommendationService的功能
       * video_id: 对应视频的唯一ID，主键
       * title: 视频标题
       * description: 视频简介
       * tag: 视频的标签列表
       * uploader_id: 上传视频的用户ID
       * views: 视频播放量
       * likes: 视频点赞量
       * favorites: 视频收藏量
       * visible: 视频是否对大众可见
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."video_info_table" (
            video_id SERIAL NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            description TEXT NOT NULL,
            tag TEXT NOT NULL,
            uploader_id INT NOT NULL,
            views INT NOT NULL DEFAULT 0,
            likes INT NOT NULL DEFAULT 0,
            favorites INT NOT NULL DEFAULT 0,
            visible BOOLEAN NOT NULL DEFAULT true
        );
         
        """,
        List()
      )
      /** 记录用户观看视频的详细信息
       * watch_id: 用户观看记录的唯一ID
       * user_id: 观看用户的ID
       * video_id: 观看的视频ID
       * watch_duration: 用户观看视频的时长（秒）
       * created_at: 记录添加时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."video_vector_table" (
            video_id SERIAL NOT NULL PRIMARY KEY,
            embedding VECTOR(1536)
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
    