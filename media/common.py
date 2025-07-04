import os
from minio import Minio

# 公共配置
MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "minio:5003")
MINIO_ACCESS_KEY = os.getenv("MINIO_ROOT_USER")
MINIO_SECRET_KEY = os.getenv("MINIO_ROOT_PASSWORD")
CALLBACK_AVATAR_API = os.getenv("CALLBACK_AVATAR_API")
CALLBACK_AVATAR_API_NAME = os.getenv("CALLBACK_AVATAR_API_NAME")
CALLBACK_COVER_API = os.getenv("CALLBACK_COVER_API")
CALLBACK_COVER_API_NAME = os.getenv("CALLBACK_COVER_API_NAME")
CALLBACK_VIDEO_API = os.getenv("CALLBACK_VIDEO_API")
CALLBACK_VIDEO_API_NAME = os.getenv("CALLBACK_VIDEO_API_NAME")

# 初始化MinIO客户端
minio_client = Minio(
    MINIO_ENDPOINT,
    access_key=MINIO_ACCESS_KEY,
    secret_key=MINIO_SECRET_KEY,
    secure=False
)

def create_buckets():
    """创建所需存储桶"""
    buckets = ["avatar", "video-cover", "video-server", "temp"]
    for bucket in buckets:
        try:
            if not minio_client.bucket_exists(bucket):
                minio_client.make_bucket(bucket)
                print(f"Created bucket: {bucket}")
        except Exception as e:
            print(f"Error creating bucket {bucket}: {str(e)}")