package Objects.UserService

case class UploadSession(
                          token: String,          // 唯一标识符（与键相同）
                          userID: Int,
                          objectName: String,     // Minio中的文件路径（如 "temp/token123/filename.jpg"）
                          createdAt: Long = System.currentTimeMillis(), // 创建时间戳
                          completed: Boolean = false // 是否已完成处理
                        )