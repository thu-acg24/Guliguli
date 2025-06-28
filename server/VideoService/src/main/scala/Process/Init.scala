
package Process

import Common.API.{API, PlanContext, TraceID}
import Common.DBAPI.{initSchema, writeDB}
import Common.ServiceUtils.schemaName
import Global.ServerConfig
import cats.effect.IO
import io.circe.generic.auto.*
import java.util.UUID
import Global.DBConfig
import Process.ProcessUtils.server2DB
import Global.GlobalVariables

object Init {
  def init(config: ServerConfig): IO[Unit] = {
    given PlanContext = PlanContext(traceID = TraceID(UUID.randomUUID().toString), 0)
    given DBConfig = server2DB(config)

    val program: IO[Unit] = for {
      _ <- IO(GlobalVariables.isTest=config.isTest)
      _ <- API.init(config.maximumClientConnection)
      _ <- Common.DBAPI.SwitchDataSourceMessage(projectName = Global.ServiceCenter.projectName).send
      _ <- initSchema(schemaName)
            /** 记录用户对视频的点赞信息
       * user_id: 点赞用户的ID
       * video_id: 点赞的视频ID
       * timestamp: 点赞时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."like_record_table" (
            user_id SERIAL NOT NULL PRIMARY KEY,
            video_id INT NOT NULL,
            timestamp TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 收藏记录表，存储用户收藏视频的情况
       * user_id: 收藏用户的ID
       * video_id: 收藏的视频ID
       * timestamp: 收藏时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."favorite_record_table" (
            user_id SERIAL NOT NULL PRIMARY KEY,
            video_id INT NOT NULL,
            timestamp TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      /** 视频表，包含上传视频的基础信息
       * video_id: 视频的唯一ID，主键，自增
       * title: 视频标题
       * description: 视频简介
       * duration: 时长（秒）
       * tag: 视频标签列表
       * server_path: 视频存储路径
       * cover_path: 视频封面路径
       * uploader_id: 上传者用户ID
       * views: 视频播放量
       * likes: 视频点赞量
       * favorites: 视频收藏量
       * status: 视频审核状态
       * upload_time: 视频上传时间
       * last_modified_time: 最近修改时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."video_table" (
            video_id SERIAL NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            description TEXT,
            duration INT NOT NULL,
            tag TEXT NOT NULL,
            server_path TEXT NOT NULL,
            cover_path TEXT NOT NULL,
            uploader_id INT NOT NULL,
            views INT NOT NULL DEFAULT 0,
            likes INT NOT NULL DEFAULT 0,
            favorites INT NOT NULL DEFAULT 0,
            status TEXT NOT NULL DEFAULT 'Pending',
            upload_time TIMESTAMP NOT NULL,
            last_modified_time TIMESTAMP
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
    