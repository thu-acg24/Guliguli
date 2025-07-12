# Guliguli 文件系统架构 - Media功能说明

## 概述

Guliguli 项目的文件系统采用 minIO 进行存储，文件处理服务使用 Flask，本服务提供图片和视频处理功能，包括图片格式转换（转JPG）和视频转码切片（HLS格式）。服务与MinIO对象存储集成，使用GPU加速视频处理。

## 功能特性

### 图片处理

- 支持格式：JPG, PNG, WEBP → 统一转换为JPG
- 尺寸限制：最大不超过2x2048x2048像素数（暂时没有裁剪/缩放功能）
- 存储位置：`avatar` 或 `video-cover` 桶
- 路径格式：`{user_id}/{uuid}.jpg`

### 视频处理

- 支持格式：常见视频格式（MP4, MOV, AVI等）
- 验证条件：
  - 分辨率：100x100 到 3840x2160
  - 时长：1秒 到 60分钟
- 输出格式：HLS（.m3u8 + .ts切片）
- GPU加速：使用NVIDIA CUDA硬件加速（目前已知的Bug: 使用Aviutl的x265/nvenc等插件导出的视频能够使用cpu处理，但是不能使用nvenc进行转码，这是目前唯一一个遇到的会报failure的情况）
- 存储位置：`video-server` 桶
- 路径格式：`{video_id}/{video_id}/{video_name}/index.m3u8`，其中 `video_name` 为我们的 `VideoService` 后端生成的名字，格式为 `[timestamp]_[uuid]`，因此唯一性可以保证。两个 `video_id` 为历史遗留问题。暂时没有修复。

## API端点

### 图片处理

- **URL**: `/image`

- **方法**: `POST`

- **参数**:

  - `token`: 回调令牌
  - `id`: 用户ID
  - `file_name`: MinIO 临时桶中的文件名
  - `task`: 任务类型 (`avatar` 或 `cover`)

- **响应**:

  json

  ```
  {
    "status": "success" | "failure",
    "message": "存储路径或错误信息"
  }
  ```

- **回调**:
  - `url`: 会自动根据 `request.headers.get('X-Forwarded-For')` 来获取请求端ip，所以要确保能传入media能够访问的ip，api name硬编码为 `Confirm[task]Message`，端口也是硬编码的
  - `token`: 将得到的Session token原封不动传回，服务端校验用
  - `status`: 为 `success` 和 `failure` 中的一个，表示处理是否成功
  - `objectName`: 转换完毕的文件在 MinIO bucket 中的文件名（bucket 为硬编码，不包含在 `objectName` 中）

### 视频处理

- **URL**: `/video`

- **方法**: `POST`

- **参数**:

  - `id`: 用户ID
  - `token`: 回调令牌
  - `file_name`: MinIO临时桶中的文件名

- **响应**:

  json

  ```
  {
    "status": "success" | "failure",
    "message": "处理状态信息"
  }
  ```

## 回调接口

视频处理完成后会调用配置的`CALLBACK_API`（现在被硬编码为 `ConfirmVideoMessage`），回调数据结构：

```
{
  "token": "原始令牌",
  "status": "success" | "failure",
  "m3u8_path": "HLS主文件路径",
  "ts_path_prefix": "TS文件前缀",
  "ts_count": 切片数量,
  "duration": 视频长度
  "serviceName": "Tong-Wen",
  "message_type": "回调API的Route"
}
```

## 测试

可以直接使用 POST 命令来测试 media-processor。由于 session token 必然不合法，这种测试理应不会对服务造成任何影响。不过我们还没有做好回调失败后在 MinIO 端的垃圾处理，所以请自行删除垃圾文件。

### 图片处理测试

```bash
curl -X POST http://localhost:5000/image \
  -F "id=user123" \
  -F "file_name=avatar.png" \
  -F "task=avatar"
```

### 视频处理测试

```bash
curl -X POST http://localhost:5000/video \
  -F "id=user456" \
  -F "token=abc123-xyz" \
  -F "file_name=video.mp4"
```