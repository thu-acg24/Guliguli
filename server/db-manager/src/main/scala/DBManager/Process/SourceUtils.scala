package DBManager.Process

import Global.DBConfig
import Global.ServiceCenter
import cats.effect.*
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import java.sql.Connection

object SourceUtils {
  def createDataSource(c: DBConfig, tail: String): IO[HikariDataSource] = {
    val config = new HikariConfig()
    config.setJdbcUrl(c.jdbcUrl + tail)
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

    IO {
      new HikariDataSource(config)
    }
  }


  def initDB(connection: Connection, dbName: String): Unit = {
    // Wrap database operations in IO to manage side effects
    val dbExistsQuery = s"SELECT 1 FROM pg_database WHERE datname = ?" // Use prepared statements to prevent SQL injection
    val dbExistsStatement = connection.prepareStatement(dbExistsQuery)
    try {
      dbExistsStatement.setString(1, dbName)
      val dbExistsResult = dbExistsStatement.executeQuery()
      // Check if the database exists and create it if it doesn't
      if (!dbExistsResult.next()) {
        val createDbStatement = connection.createStatement()
        try {
          createDbStatement.executeUpdate(s"CREATE DATABASE $dbName")
        } finally {
          createDbStatement.close() // Ensure statement is closed after execution
        }
      }
    } finally {
      dbExistsStatement.close() // Ensure prepared statement is closed after execution
    }
  }



  def createDataSource(dbConfig: DBConfig): HikariDataSource = {
    val config = new HikariConfig()
    config.setJdbcUrl(dbConfig.jdbcUrl)
    config.setUsername(dbConfig.username)
    config.setPassword(dbConfig.password)
    config.addDataSourceProperty("cachePrepStmts", true)
    config.addDataSourceProperty("prepStmtCacheSize", dbConfig.prepStmtCacheSize)
    config.addDataSourceProperty("prepStmtCacheSqlLimit", dbConfig.prepStmtCacheSqlLimit)

    // Set max lifetime to 30 minutes (1800000 milliseconds)
    config.setMaxLifetime(1800000);

    // Set idle timeout to 10 minutes (600000 milliseconds)
    config.setIdleTimeout(600000);
    config.setMaximumPoolSize(dbConfig.maximumPoolSize)

    new HikariDataSource(config)
  }

}
