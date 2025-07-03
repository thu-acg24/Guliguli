import os
import uuid
import shutil
import subprocess
from flask import Flask, request, jsonify
from minio import Minio
from minio.error import S3Error
import imageio.v3 as iio
from werkzeug.utils import secure_filename

app = Flask(__name__)

# 配置信息（建议从环境变量获取）
MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT", "minio:9000")
MINIO_ACCESS_KEY = os.getenv("MINIO_ROOT_USER")
MINIO_SECRET_KEY = os.getenv("MINIO_ROOT_PASSWORD")
MINIO_BUCKET = os.getenv("MINIO_BUCKET", "temp")
CALLBACK_API = os.getenv("CALLBACK_API")

# 确保关键变量存在
if not all([MINIO_ACCESS_KEY, MINIO_SECRET_KEY, CALLBACK_API]):
    raise EnvironmentError("Missing required environment variables")

# 初始化MinIO客户端
minio_client = Minio(
    MINIO_ENDPOINT,
    access_key=MINIO_ACCESS_KEY,
    secret_key=MINIO_SECRET_KEY,
    secure=False
)
def process_image(file_path, task_type):
    """处理图片文件"""
    try:
        # 验证图片合法性
        img = iio.imread(file_path)
        
        # 检查图片尺寸（示例：最大5000x5000）
        if img.shape[0] > 5000 or img.shape[1] > 5000:
            raise ValueError("Image dimensions too large")
            
        # 确定目标文件夹
        folder = "avatar" if task_type == "avatar" else "cover"
        new_filename = f"{folder}/{uuid.uuid4().hex}.{file_path.split('.')[-1]}"
        
        # 上传到MinIO
        minio_client.fput_object(
            MINIO_BUCKET,
            new_filename,
            file_path
        )
        return new_filename, None
        
    except Exception as e:
        raise RuntimeError(f"Image processing failed: {str(e)}")

def process_video(file_path):
    """处理视频文件"""
    try:
        base_name = os.path.basename(file_path).split('.')[0]
        output_dir = f"/tmp/{uuid.uuid4().hex}"
        os.makedirs(output_dir, exist_ok=True)
        
        # 使用FFmpeg转码和切片
        m3u8_file = f"{output_dir}/{base_name}.m3u8"
        ts_pattern = f"{output_dir}/{base_name}_%05d.ts"
        
        cmd = [
            "ffmpeg",
            "-hwaccel", "cuda",         # 启用 CUDA 硬件加速
            "-hwaccel_output_format", "cuda",  # 设置输出格式
            "-i", file_path,
            "-c:v", "h264_nvenc",       # 使用 NVIDIA H.264 编码器
            "-preset", "p4",            # Windows 下建议使用 p4 预设
            "-profile:v", "main",       # H.264 profile
            "-level", "4.1",            # H.264 level
            "-b:v", "5M",               # 视频码率
            "-maxrate", "10M",          # 最大码率
            "-bufsize", "10M",          # 缓冲区大小
            "-c:a", "aac",              # 音频编码
            "-b:a", "128k",             # 音频码率
            "-hls_time", "10",          # HLS 片段时长
            "-hls_list_size", "0",      # 无限播放列表
            "-hls_segment_filename", ts_pattern,
            "-f", "hls",
            m3u8_file
        ]
        
        # 添加超时处理（Windows 下更稳定）
        try:
            result = subprocess.run(
                cmd, 
                check=True,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                timeout=1800  # 30分钟超时
            )
        except subprocess.TimeoutExpired:
            raise RuntimeError("FFmpeg processing timed out after 30 minutes")
        
        # 收集生成的TS文件
        ts_files = [f for f in os.listdir(output_dir) if f.endswith('.ts')]
        
        # 上传所有文件到MinIO
        uploaded_files = []
        for filename in [os.path.basename(m3u8_file)] + ts_files:
            remote_path = f"video/{filename}"
            minio_client.fput_object(
                MINIO_BUCKET,
                remote_path,
                os.path.join(output_dir, filename)
            )
            uploaded_files.append(remote_path)
        
        return os.path.basename(m3u8_file), uploaded_files
    
    except subprocess.CalledProcessError as e:
        error_msg = f"FFmpeg failed with code {e.returncode}:\n{e.stderr.decode('utf-8') if isinstance(e.stderr, bytes) else e.stderr}"
        print(error_msg)
        raise RuntimeError(error_msg)
    
    except Exception as e:
        raise RuntimeError(f"Video processing failed: {str(e)}")
    finally:
        if os.path.exists(output_dir):
            shutil.rmtree(output_dir)

@app.route('/process', methods=['POST'])
def handle_process():
    # 获取请求参数
    file_name = secure_filename(request.form.get('file_name'))
    task = request.form.get('task')
    token = request.form.get('token')
    
    if not all([file_name, task, token]):
        return jsonify({"error": "Missing parameters"}), 400

    try:
        # 临时文件路径
        local_path = f"/tmp/{uuid.uuid4().hex}_{file_name}"
        
        # 从MinIO下载文件
        minio_client.fget_object(MINIO_BUCKET, file_name, local_path)
        
        # 根据任务类型处理
        if task in ["avatar", "cover"]:
            result, ts_files = process_image(local_path, task)
            result_type = "image"
        elif task == "video":
            result, ts_files = process_video(local_path)
            result_type = "video"
        else:
            return jsonify({"error": "Invalid task type"}), 400
        
        # 删除原始文件
        minio_client.remove_object(MINIO_BUCKET, file_name)
        os.remove(local_path)
        
        # 准备回调数据
        callback_data = {
            "token": token,
            "type": result_type,
            "main_file": result,
            "ts_files": ts_files or []
        }
        
        # 此处应添加实际回调API的请求代码
        # requests.post(CALLBACK_API, json=callback_data, timeout=10)
        print(f"Would callback with: {callback_data}")
        
        return jsonify({"status": "success"}), 200
        
    except S3Error as e:
        return jsonify({"error": f"MinIO error: {str(e)}"}), 500
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, threaded=True)