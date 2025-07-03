# Guliguli 微服务架构 - Service文件夹结构说明

## 概述

Guliguli 项目采用微服务架构，每个服务负责特定的业务功能。所有服务均使用 Scala 3.4.2 开发，基于 SBT 构建系统，遵循统一的项目结构和开发规范。

## 服务列表

| 服务名称 | 功能描述 | 端口 | 状态 |
|---------|----------|------|------|
| VideoService | 视频管理服务 | - | 活跃 |
| UserService | 用户管理服务 | - | 活跃 |
| CommentService | 评论服务 | - | 活跃 |
| DanmakuService | 弹幕服务 | - | 活跃 |
| MessageService | 消息服务 | - | 活跃 |
| HistoryService | 历史记录服务 | - | 活跃 |
| RecommendationService | 推荐服务 | - | 活跃 |
| ReportService | 举报服务 | - | 活跃 |

## 统一项目结构

每个微服务都遵循以下标准目录结构：

```
ServiceName/
├── build.sbt                    # SBT 构建配置文件
├── projectInfo.yaml            # 项目信息配置（项目ID、版本等）
├── server_config.json          # 服务运行时配置
├── project/                    # SBT 项目配置目录
│   ├── build.properties       # SBT 版本配置
│   ├── plugins.sbt            # SBT 插件配置
│   └── target/                # 编译临时文件
├── src/main/                   # 主要源代码目录
│   ├── scala/                  # Scala 源代码
│   │   ├── APIs/              # API 消息定义
│   │   ├── Common/            # 通用工具和基础类
│   │   ├── Global/            # 全局配置和常量
│   │   ├── Impl/              # API 具体实现（Planner）
│   │   ├── Objects/           # 数据对象定义
│   │   ├── Process/           # 服务启动和处理逻辑
│   │   └── Utils/             # 工具类
│   └── resources/             # 资源文件
└── target/                     # 编译输出目录
```

## 核心目录详解

### 1. APIs/ 目录 - API 消息定义

定义所有对外提供的 API 接口消息格式。每个 API 消息都是一个 case class，继承自 `API[T]`。

**命名规范：**
- 文件名：`{动作}{对象}Message.scala`
- 类名：与文件名相同

**消息类型：**
- `Query*Message` - 查询类操作
- `Change*Message` - 修改类操作  
- `Upload*Message` - 上传类操作
- `Delete*Message` - 删除类操作
- `Modify*Message` - 更新类操作

**标准结构示例：**
```scala
package APIs.ServiceName

import Common.API.API
import Global.ServiceCenter.ServiceNameCode

/**
 * API描述
 * @param param1 参数1描述
 * @param param2 参数2描述
 */
case class ApiMessage(
  param1: String,
  param2: Int
) extends API[ReturnType](ServiceNameCode)

case object ApiMessage {
  // Circe + Jackson 双重序列化支持
  // ... 序列化代码
}
```

### 2. Impl/ 目录 - API 实现

包含所有 API 的具体实现逻辑，每个 API 对应一个 Planner。

**命名规范：**
- 文件名：`{API消息名}Planner.scala`
- 类名：`{API消息名}Planner`

**标准结构：**
```scala
package Impl

import Common.API.{PlanContext, Planner}
import cats.effect.IO

case class ApiMessagePlanner(
  // API 参数
  override val planContext: PlanContext
) extends Planner[ReturnType] {
  
  override def plan(using PlanContext): IO[ReturnType] = {
    // 业务逻辑实现
  }
  
  // 私有辅助方法
}
```

## 其他目录

### 3. Common/ 目录 - 通用组件

包含跨服务共享的基础组件和工具类：

- `API/` - API 基础类和上下文
- `DBAPI/` - 数据库访问接口
- `Object/` - 通用数据对象
- `Serialize/` - 序列化工具
- `ServiceUtils/` - 服务工具函数

### 4. Global/ 目录 - 全局配置

- `ServiceCenter/` - 服务注册和发现
- 配置常量和枚举定义
- 全局异常定义

### 5. Objects/ 目录 - 数据对象

定义业务数据模型和数据库实体映射。

### 6. Process/ 目录 - 服务进程

- `Server.scala` - 服务启动入口
- 服务初始化和生命周期管理

### 7. Utils/ 目录 - 工具类

服务特定的工具函数和辅助类。

## 开发规范

### 代码风格

- 使用 Scala 3 语法特性
- 遵循函数式编程原则
- 使用 cats-effect IO 处理副作用
- 保持代码的不可变性
- 添加充分的注释和文档

### 测试规范

- 为每个 API 编写单元测试
- 使用模拟数据库进行集成测试
- 确保异常场景的覆盖
