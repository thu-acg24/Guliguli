package Process

import Global.DBConfig
import cats.effect.*
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import java.sql.Connection

object SourceUtils {
  def createDataSource(c: DBConfig, init:Boolean): Resource[IO, HikariDataSource] = {
    val config = new HikariConfig()
    config.setJdbcUrl(c.jdbcUrl+c.schemaName)
    config.setUsername(c.username)
    config.setPassword(c.password)
    config.addDataSourceProperty("cachePrepStmts", true)
    config.addDataSourceProperty("prepStmtCacheSize", c.prepStmtCacheSize)
    config.addDataSourceProperty("prepStmtCacheSqlLimit", c.prepStmtCacheSqlLimit)

    // Set max lifetime to 30 minutes (1800000 milliseconds)
    config.setMaxLifetime(1800000);

    // Set idle timeout to 10 minutes (600000 milliseconds)
    config.setIdleTimeout(600000);
    config.setMaximumPoolSize(c.maximumPoolSize)

    Resource.make(IO(new HikariDataSource(config)))(ds => IO(ds.close()))
  }
}
