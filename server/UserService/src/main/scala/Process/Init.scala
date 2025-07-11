
package Process


import Common.API.API
import Common.API.PlanContext
import Common.API.TraceID
import Common.DBAPI.initSchema
import Common.DBAPI.writeDB
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Global.GlobalVariables
import Global.ServerConfig
import cats.effect.IO
import io.circe.generic.auto.*
import org.joda.time.DateTime

import java.util.UUID
import scala.concurrent.duration.DurationInt

object Init {
  private def cleanTokenLoop(using PlanContext): IO[Unit] = {
    (IO.sleep(6.hours) >> cleanExpiredTokens).handleErrorWith { e =>
      IO.println(s"[Error] 清理Token时发生错误")
      >> IO(e.printStackTrace())
    }
    >> cleanTokenLoop
  }

  private def cleanExpiredTokens(using PlanContext): IO[String] = {
    for {
      _ <- IO.println("Executing cleaning task")
      now <- IO(DateTime.now().getMillis.toString)
      result <- writeDB(s"""DELETE FROM "$schemaName"."token_table" WHERE expiration_time <= ?;""",
        List(SqlParameter("DateTime", now)))
    } yield result
  }

  def init(config: ServerConfig): IO[Unit] = {
    given PlanContext = PlanContext(traceID = TraceID(UUID.randomUUID().toString), 0)

    val program: IO[Unit] = for {
      _ <- IO(GlobalVariables.isTest=config.isTest)
      _ <- API.init(config.maximumClientConnection)
      _ <- Common.DBAPI.SwitchDataSourceMessage(projectName = Global.ServiceCenter.projectName).send
      _ <- initSchema(schemaName)
            /** 关注关系表，记录用户之间的关注关系
       * follower_id: 关注者用户ID
       * followee_id: 被关注者用户ID
       * timestamp: 创建关注关系的时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "$schemaName"."follow_relation_table" (
            follow_relation_id SERIAL NOT NULL PRIMARY KEY,
            follower_id INT NOT NULL,
            followee_id INT NOT NULL,
            timestamp TIMESTAMP NOT NULL
        );
        """,
        List()
      )
      _ <- writeDB(
        s"""
        CREATE INDEX IF NOT EXISTS idx_follow_relation_timestamp
          ON "$schemaName"."follow_relation_table" (timestamp);
        """,
        List()
      )
      /** 用户表，包含用户基本信息
       * user_id: 用户唯一ID, 主键，自增
       * username: 用户名，唯一
       * email: 邮箱地址，唯一
       * password_hash: 用户密码的哈希值
       * bio: 用户个性签名
       * avatar_path: 用户头像路径
       * user_role: 用户角色，可能的值有Admin, Auditor, Normal
       * video_count: 用户上传的视频数量
       * is_banned: 用户是否被封禁
       * created_at: 用户创建时间
       * updated_at: 最近一次更新的时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "$schemaName"."user_table" (
            user_id SERIAL NOT NULL PRIMARY KEY,
            username TEXT NOT NULL,
            email TEXT NOT NULL,
            password_hash TEXT NOT NULL,
            bio TEXT NOT NULL DEFAULT 'Gugugaga!',
            avatar_path TEXT NOT NULL DEFAULT 'default.jpg',
            user_role TEXT NOT NULL DEFAULT 'Normal',
            video_count INT NOT NULL DEFAULT 0,
            is_banned BOOLEAN NOT NULL DEFAULT false,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
         
        """,
        List()
      )
      /** 存储用户的会话Token信息，用于用户会话管理
       * token: 用户的会话Token，主键，必须唯一
       * user_id: Token关联的用户ID
       * expiration_time: Token的过期时间
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "$schemaName"."token_table" (
            token VARCHAR NOT NULL PRIMARY KEY,
            user_id INT NOT NULL,
            expiration_time TIMESTAMP NOT NULL
        );
         
        """,
        List()
      )
      _ <- cleanTokenLoop.start
    } yield ()

    program.handleErrorWith(err => IO {
      println("[Error] Process.Init.init 失败, 请检查 db-manager 是否启动及端口问题")
      err.printStackTrace()
    })
  }
}
    