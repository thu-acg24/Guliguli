package Global

import io.circe.{Decoder, HCursor}
import io.circe.generic.semiauto.*

// * @param isRegistered 是否在后端已经注册，如果注册过则只有 model 参数生效 y/n
/**
 * 模型服务信息，前端发送补全请求时的参数
 *
 * @param model        模型名称
 * @param apiBase      模型请求地址
 * @param key          模型请求密钥
 * @param version      模型版本
 * @param modelType    模型类型，custom 或者其他，默认 custom，需要设置 model 和 apiBase; 如果不是 custom，则只需要设置 model,其余参数无效
 * @param maxToken     最大生成 token 数量
 * @param stream       是否使用流式请求
 * @param temperature  温度
 * @param maxInputTokens 最大输入 token 数量
 * @param keyId       密钥 ID - aws.accessKeyId                       
 */
case class ModelServiceInfo(
                             model: String, 
                             apiBase: String,
                             key: String = "", 
                             version: String = "2023-05-15", 
                             modelType: String = "custom", 
                             maxToken: Int = 28192, 
                             stream: Boolean = true, 
                             host: String = "", 
                             port: Int, 
                             temperature: Double = 0.5, 
                             maxInputTokens: Int = 128000,
                             keyId: String = "",
                           ) {
  // 优先从环境变量中获取，如果没有则使用配置文件中的值
  lazy val _key: String = {
    val envKey = s"$model_KEY"
    val keyS = sys.env.getOrElse(envKey, "")
    if (keyS.isEmpty) { key } 
    else { keyS }
  }

}


case object ModelServiceInfo {
  given Decoder[ModelServiceInfo] = new Decoder[ModelServiceInfo] {
    final def apply(c: HCursor): Decoder.Result[ModelServiceInfo] =
      for {
        model <- c.downField("model").as[String]
        apiBase <- c.downField("apiBase").as[Option[String]].map(_.getOrElse(""))
        key <- c.downField("key").as[Option[String]].map(_.getOrElse(""))
        keyId <- c.downField("keyId").as[Option[String]].map(_.getOrElse(""))
        version <- c.downField("version").as[Option[String]].map(_.getOrElse("2023-05-15"))
        modelType <- c.downField("modelType").as[Option[String]].map(_.getOrElse("custom"))
        maxToken <- c.downField("maxToken").as[Option[Int]].map(_.getOrElse(28192))
        stream <- c.downField("stream").as[Option[Boolean]].map(_.getOrElse(true))
        host <- c.downField("host").as[Option[String]].map(_.getOrElse(""))
        port <- c.downField("port").as[Option[Int]].map(_.getOrElse(2080))
      } yield {
        ModelServiceInfo(model, apiBase, key, version, modelType, maxToken, stream, host, port, keyId = keyId)
      }
  }
}

object ServerConfigDecoder {
  implicit val serverConfigDecoder: Decoder[ServerConfig] = deriveDecoder[ServerConfig]

  def parser(jsonString: String): Either[io.circe.Error, ServerConfig] = {
    io.circe.parser.decode[ServerConfig](jsonString)
  }
}

/** 配置文件 */
  case class ServerConfig(
                         /** 服务器地址 */
                         serverIP: String,

                         /** 服务器端口 */
                         serverPort: Int,

                         /** 最大连接数 */
                         maximumServerConnection: Int,

                         /** 最大的同时往内部微服务发送的请求个数，原则上和最大连接数相同 */
                         maximumClientConnection: Int,

                         /** 数据库地址，例如：jdbc:postgresql://localhost:5432/db */
                         jdbcUrl: String,

                         /** 用户名 */
                         username: String,

                         /** 密码 */
                         password: String,

                         /** 缓存的数据库statement个数 */
                         prepStmtCacheSize: Int,

                         /** 缓存的数据库语句最大长度 */
                         prepStmtCacheSqlLimit: Int,

                         /** 最多能够保持的连接数目，建议=服务器的CPU核数*2+1 */
                         maximumPoolSize: Int,

                         /** connection的最长存活时间 */
                         connectionLiveMinutes: Int,

                          /** 模型服务配置 */
                         modelServices: Map[String, ModelServiceInfo],

                         /** 模型代理配置 */
                         proxyConfig: String,

                         proxyHost: String,

                         useProxy: Boolean,

                         /** 测试服务器地址 */
                         testServer:String,
    
                         userID:String,
                         sbtPath: String,
                         sbtArgs: List[String]
                       ) {
  
  def sbtCompileCommand: Seq[String] =       // 关键：使用 Seq 拼接路径和参数
    Seq(this.sbtPath) ++ this.sbtArgs
}