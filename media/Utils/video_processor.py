import threading
import subprocess
import hashlib
import json
import requests
from flask import Blueprint, request, jsonify
from minio.error import S3Error
import os
import shutil
import sys
import logging

from common import minio_client, CALLBACK_VIDEO_API_NAME

video_bp = Blueprint('video_bp', __name__)
    
def validate_video(file_path):
    """验证视频文件合法性"""
    try:
        # 使用FFprobe检查视频文件
        cmd = [
            "ffprobe",
            "-v", "error",
            "-select_streams", "v:0",
            "-show_entries", "stream=codec_name,width,height,duration",
            "-of", "json",
            file_path
        ]
        
        result = subprocess.run(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=30  # 30秒超时
        )
        
        if result.returncode != 0:
            raise RuntimeError(f"FFprobe error: {result.stderr.decode()}")
        
        # 解析视频信息
        video_info = json.loads(result.stdout)
        video_bp.logger.info(f"video_info: {video_info}")
        streams = video_info.get("streams", [])
        
        if not streams:
            raise ValueError("No video stream found")
        
        stream = streams[0]
        width = int(stream.get("width", 0))
        height = int(stream.get("height", 0))
        duration = float(stream.get("duration", 0))

        video_bp.logger.info(f"width: {width}, height: {height}, duration: {duration}")
        
        # 验证视频参数
        if width < 100 or height < 100:
            raise ValueError("Video resolution too small (min 100x100)")
        
        if width > 3840 or height > 2160:
            raise ValueError("Video resolution too large (max 3840x2160)")
        
        if duration < 1:
            raise ValueError("Video too short (min 1 second)")
        
        if duration > 3600:  # 60分钟
            raise ValueError("Video too long (max 60 minutes)")
        
        return duration
        
    except Exception as e:
        video_bp.logger.info(f"Video validation failed: {str(e)}")
        raise RuntimeError(f"Video validation failed: {str(e)}")

def process_video_async(video_id, token, file_name, local_path, duration, target_ip):
    """异步处理视频转码和切片"""
    try:
        # 生成唯一视频ID作为前缀
        video_prefix = f"{video_id}/{file_name.split('.')[0]}"
        output_dir = f"/tmp/{video_prefix}"
        os.makedirs(output_dir, exist_ok=True)
        
        # 设置输出文件路径
        m3u8_file = f"{output_dir}/index.m3u8"
        ts_pattern = f"{output_dir}/segment_%05d.ts"

        video_bp.logger.info(f"m3u8: {m3u8_file}, ts_pattern: {ts_pattern}")
        
        # 转码命令
        cmd = [
            "ffmpeg",
            "-hwaccel", "cuda",                 # 启用CUDA硬件加速
            "-hwaccel_output_format", "cuda",   # 设置输出格式
            "-i", local_path,
            "-c:v", "h264_nvenc",               # 使用NVIDIA H.264编码器
            "-preset", "medium",                  # Win-p4, Linux下推荐使用slow/medium/fast等预设
            "-profile:v", "main",               # H.264 profile
            "-level", "4.1",                    # H.264 level
            "-b:v", "5M",                       # 视频码率
            "-maxrate", "10M",                  # 最大码率
            "-bufsize", "10M",                  # 缓冲区大小
            "-c:a", "aac",                      # 音频编码
            "-b:a", "128k",                     # 音频码率
            "-hls_time", "10",                  # HLS片段时长
            "-hls_list_size", "0",              # 无限播放列表
            "-hls_segment_filename", ts_pattern,
            "-f", "hls",
            m3u8_file
        ]

        video_bp.logger.info(f"cmd: {cmd}")
        
        # 执行转码
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE
        )
        
        # 等待转码完成
        stdout, stderr = process.communicate(timeout=3600)  # 1小时超时
        
        if process.returncode != 0:
            raise RuntimeError(f"FFmpeg failed: {stderr.decode()}")
        
        # 获取生成的ts文件列表
        ts_files = [f for f in os.listdir(output_dir) if f.endswith('.ts')]
        ts_count = len(ts_files)
        
        # 上传所有文件到MinIO
        for filename in ["index.m3u8"] + ts_files:
            remote_path = f"{video_prefix}/{filename}"
            minio_client.fput_object(
                "video-server",
                remote_path,
                os.path.join(output_dir, filename)
            )
        
        # 准备回调数据
        callback_data = {
            "sessionToken": token,
            "status": "success",
            "m3u8Name": f"{video_prefix}/index.m3u8",
            "tsPrefix": f"{video_prefix}/segment",
            "sliceCount": ts_count,
            "duration": duration
        }
        
        # 发送回调请求
        questPost(callback_data, target_ip)
        
    except Exception as e:
        # 准备错误回调数据
        video_bp.logger.info(f"Video process failed: {str(e)}")
        callback_data = {
            "token": token,
            "status": "failure",
            "m3u8Name": "",
            "tsPrefix": "",
            "sliceCount": 0,
            "duration": 0
        }
        questPost(callback_data, target_ip)
        
    finally:
        # 清理资源
        try:
            minio_client.remove_object("temp", file_name)
        except:
            pass
        
        try:
            os.remove(local_path)
        except:
            pass
        
        try:
            if os.path.exists(output_dir):
                shutil.rmtree(output_dir)
        except:
            pass

def questPost(data: dict, target_ip: str):
    """发送回调请求到API"""
    # 添加服务标识
    data["serviceName"] = "Tong-Wen"
    data["message_type"] = CALLBACK_VIDEO_API_NAME
    
    # 计算哈希值
    body_str = json.dumps(data, separators=(',', ':'))
    x_hash = hashlib.md5(body_str.encode('utf-8')).hexdigest()
    
    headers = {
        "Content-Type": "application/json",
        "X-Hash": x_hash,
    }
    
    callback_api = f"http://{target_ip}:10016/api/{CALLBACK_VIDEO_API_NAME}"

    try:
        video_bp.logger.info(f"callback: url={callback_api}, headers={headers}, data={body_str}")
        response = requests.post(
            url=callback_api,
            headers=headers,
            data=body_str,
            timeout=10
        )
        video_bp.logger.info(f"Callback Status: {response.status_code}, Response: {response.text}")
    except requests.exceptions.RequestException as e:
        video_bp.logger.info(f"Callback failed: {str(e)}")

@video_bp.route('/video', methods=['POST'])
def handle_video():
    # 获取请求参数
    video_id = request.json.get('id')
    token = request.json.get('token')
    file_name = request.json.get('file_name')
    # target_ip = request.headers.get('X-Real-IP', request.remote_addr)
    # for key, value in request.headers.items():
    #     video_bp.logger.info(f"{key}: {value}")
    forwarded_for = request.headers.get('X-Forwarded-For')
    if forwarded_for:
        # 获取第一个 IP 地址
        client_ip = forwarded_for.split(',')[0].strip()
        video_bp.logger.info(f"Your client IP address is: {client_ip}")
        target_ip = client_ip
    else:
        # 如果没有 X-Forwarded-For 头部，尝试获取 X-Real-IP
        real_ip = request.headers.get('X-Real-IP')
        if real_ip:
            target_ip = real_ip
            video_bp.logger.info(f"Your real IP address is: {real_ip}")
        else:
            # 如果都没有，返回默认值
            return jsonify({"status": "failure", "message": "Unable to determine your IP address."}), 400

    video_bp.logger.info(f"token: {token}, video_id: {video_id}, file_name: {file_name}, target_ip: {target_ip}\n")
    
    if not all([video_id, token, file_name]):
        return jsonify({"status": "failure", "message": "Missing parameters: token, id, file_name are required"}), 400
    
    # 临时文件路径
    local_path = f"/tmp/{file_name}"
    
    try:
        # 从MinIO下载文件
        minio_client.fget_object("temp", file_name, local_path)
        
        # 验证视频文件
        duration = validate_video(local_path)
        
        # 启动异步处理线程
        threading.Thread(
            target=process_video_async,
            args=(video_id, token, file_name, local_path, duration, target_ip),
            daemon=True
        ).start()
        
        return jsonify({
            "status": "success",
            "message": "Video processing started"
        }), 200
        
    except S3Error as e:
        return jsonify({
            "status": "failure",
            "message": f"Storage error: {str(e)}"
        }), 500
    except Exception as e:
        # 清理可能残留的文件
        if os.path.exists(local_path):
            os.remove(local_path)
        return jsonify({
            "status": "failure",
            "message": str(e)
        }), 500