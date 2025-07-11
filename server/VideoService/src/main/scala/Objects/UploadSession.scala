package Objects

case class UploadSession(
                          token: String,          // 唯一标识符（与键相同）
                          userToken: String,
                          videoID: Int,
                          objectName: String,     // Minio中的文件路径（如 "temp/token123/filename.jpg"）
                          uploadID: String = "", // 分块上传时的ID
                          completed: Boolean = false // 是否已完成上传
                        )