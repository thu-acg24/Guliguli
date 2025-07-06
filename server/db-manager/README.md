## 同文后端服务

## 基本打包
```shell
# 在项目根目录下执行
sbt Universal/packageBin

# 打包后的文件在 target/universal 目录下的 .zip 中，解压后即可 ./bin/server 运行
ls target/universal
```

## 代理设置

启动 VM 参数：

`-Dhttps.proxyHost="127.0.0.1" -Dhttps.proxyPort=2080`