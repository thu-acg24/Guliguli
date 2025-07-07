
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
      /** 评论表，包含所有视频评论的基本信息
       * comment_id: 评论表的主键ID，唯一标识一个评论
       * content: 评论内容
       * video_id: 所属视频的ID
       * author_id: 评论发布者的用户ID
       * reply_to_id: 回复的目标评论ID，可以为空
       * reply_to_user_id: 回复的目标用户ID，可以为空
       * root_id: 评论所属楼的评论ID，如果自身是一级评论则为空
       * likes: 评论的点赞数量
       * time_stamp: 评论的发布时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."comment_table" (
            comment_id SERIAL NOT NULL PRIMARY KEY,
            content TEXT NOT NULL,
            video_id INT NOT NULL,
            author_id INT NOT NULL,
            reply_to_id INT,
            reply_to_user_id INT,
            root_id INT,
            reply_count INT NOT NULL DEFAULT 0,
            likes INT NOT NULL DEFAULT 0,
            time_stamp TIMESTAMP NOT NULL
        );
        """,
        List()
      )
      _ <- writeDB(
        s"""CREATE INDEX IF NOT EXISTS idx_comment_timestamp ON "${schemaName}"."comment_table"(time_stamp);
           |""".stripMargin, List())
      _ <- writeDB(
        s"""CREATE INDEX IF NOT EXISTS idx_comment_root_id ON "${schemaName}"."comment_table"(root_id);
           |""".stripMargin, List())
      _ <- writeDB(
        s"""CREATE INDEX IF NOT EXISTS idx_comment_video_id ON "${schemaName}"."comment_table"(video_id);
           |""".stripMargin, List())
      /** 点赞评论记录表，保存用户对评论的点赞情况
       * user_id: 点赞用户的ID
       * comment_id: 点赞评论的ID
       * time_stamp: 点赞时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."like_comment_record_table" (
            like_record_id SERIAL NOT NULL PRIMARY KEY,
            user_id INT NOT NULL,
            comment_id INT NOT NULL,
            time_stamp TIMESTAMP NOT NULL
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
    