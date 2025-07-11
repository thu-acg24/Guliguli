# Guliguli

一个类型安全的小规模视频平台，后端采用 Scala，前端采用 Electron + React，项目框架由同文生成。

## Deployment

服务器端均使用

```
sbt Universal/packageBin
```

编译，编译结果位于 `target/universal` 下的压缩包内，解压后运行 `bin` 下的可执行文件启动服务或 DB Manager。也可以使用

```
sbt run
```

直接运行。

### DB Manager

安装 PostgreSQL 16 并创建名为 `guliguli` 的数据库。

将 `server/db-manager/db_config_template.json` 复制为 `db_config.json`，修改其中的数据库地址、用户名、密码等配置。在运行 `RecommendationService` 的设备上，需要额外安装 [pgvector](https://github.com/pgvector/pgvector) 扩展并确保数据库用户具有超级用户权限。

在 `server/db-manager` 下运行服务，DB Manager 会从当前目录下的 `db_config.json` 中读取信息。

### 服务器

将 `server/server-config-template.env` 复制为 `server-config.env` 并填写各个服务的地址。

参照 `media/` 文件夹下的说明部署 MinIO 和文件处理服务器，将 `server/minio-config-template.env` 复制为 `minio-config.env` 并填写 MinIO 与文件处理服务器的地址。

在 `server/[Service]` 下运行服务，服务会从当前目录下的 `server_config.json` 和上级目录的 `minio-config.env` 与 `server-config.env` 中读取配置。各个服务不共用数据库。

### 客户端

将 `client/src/server-config-template.ts` 复制为 `server-config.ts` 并填写各个服务的地址。

在 `client` 目录下，运行

```
yarn
```

安装依赖，随后运行

```
yarn make
```

构建，或者

```
yarn start
```

运行。
