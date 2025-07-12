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

### 文件系统

由于一些原因，我们的文件系统运行在 Docker 中。我们使用 docker-compose 启动容器，容器内使用基于 NVIDIA cuda 的 nvenc 来编码视频，因此本地需要有显卡驱动。如果想要使用 CPU 编码，可以自行修改 `media/Utils/video_processor.py` 中的指令。

如果部署的设备是 Windows/Mac 系统，需要使用 nginx 反向代理，将请求端 ip 发进 Docker，具体的 nginx 配置在 `media/nginx.conf` 中。如果有更好的解决方式，可以告知我，同时如果 nginx 对于某些桶配置了缓存（默认是除了 `temp` 外的所有桶，注意 `temp` 桶不能配置缓存，否则会影响 media processor），则无法使用 MinIO Console 来查看该桶中的文件。

如果是 Linux 系统，或许可以使用 Docker 的 Host 模式，此时 MinIO 的端口为 `9000-9001`，Media-processor 的端口为 `4850`，需要修改 `minio-config.env` 为合适的端口，请注意。

将 `media/template.env` 复制为 `.env`，并填写 MinIO Root 用户的账户密码、MinIO 服务的 IP 地址（由于生成自签名链接需要，外部通过什么网址调 MinIO 就要填写同样的地址。如果需要将本地服务映射到域名，请自行解决）。如果需要查看 MinIO 的 log，需要取消注释 `MINIO_LOGGER_WEBHOOK_ENABLE_LOGGER` 等字段，并在 MinIO 服务器运行 `media/webhook.py` 启动 webhook 服务来获取 MinIO 发出的 log。Media-processor 的 log 会自然地打在 docker 容器中。

使用 `docker-compose up (--build) -d` 即可在后台启动 Docker 容器。如果是 Windows 系统需要先启动 Docker Desktop 应用。

### 服务器

将 `server/server-config-template.env` 复制为 `server-config.env` 并填写各个服务的地址。

部署 MinIO 和文件处理服务器，将 `server/minio-config-template.env` 复制为 `minio-config.env` 并填写 MinIO 与文件处理服务器的地址。

在 `server/[Service]` 下运行服务，服务会从当前目录下的 `server_config.json` 和上级目录的 `minio-config.env` 与 `server-config.env` 中读取配置。各个服务不共用数据库。

对于 Windows 用户，我们提供了 `server/Utils/run_services.bat` 脚本，可以自行修改该脚本来启动需要的服务。

### 客户端

将 `client/src/server-config-template.ts` 复制为 `server-config.ts` 并填写各个服务的地址。

在 `client` 目录下，运行

```
yarn
```

安装依赖（如果安装不了，建议使用 `--network-timeout 500000` 来等待下载一些比较大的依赖），随后运行

```
yarn make
```

构建，或者

```
yarn start
```

运行。
