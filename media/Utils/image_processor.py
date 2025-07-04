from flask import Blueprint, request, jsonify
from minio.error import S3Error
from PIL import Image
import io
import uuid
import os
import threading
import hashlib
import json
import requests
import sys

from common import minio_client, CALLBACK_AVATAR_API, CALLBACK_AVATAR_API_NAME, CALLBACK_COVER_API, CALLBACK_COVER_API_NAME

def questPost(data: dict, callback_api: str, api_name: str):
    """发送回调请求到API"""
    # 添加服务标识
    data["serviceName"] = "Tong-Wen"
    data["message_type"] = api_name
    
    # 计算哈希值
    body_str = json.dumps(data, separators=(',', ':'))
    x_hash = hashlib.md5(body_str.encode('utf-8')).hexdigest()
    
    headers = {
        "Content-Type": "application/json",
        "X-Hash": x_hash,
    }
    
    try:
        print(f"callback: url={callback_api}, headers={headers}, data={body_str}")
        response = requests.post(
            url=callback_api,
            headers=headers,
            data=body_str,
            timeout=10
        )
        print(f"Callback Status: {response.status_code}, Response: {response.text}\n")
    except requests.exceptions.RequestException as e:
        print(f"Callback failed: {str(e)}\n")

def process_image(id, file_path, file_name, task_type):
    """处理图片文件并转换为JPG格式"""
    try:
        # 读取并验证原始图片
        with Image.open(file_path) as img:
            # 验证图片尺寸（最大1024x1024）
            if img.width > 1024 or img.height > 1024:
                raise ValueError("Image dimensions too large (max 1024x1024)")
            
            # 转换图片为RGB模式（处理透明度/CMYK等情况）
            if img.mode in ('RGBA', 'LA', 'P'):
                background = Image.new('RGB', img.size, (255, 255, 255))
                background.paste(img, mask=img.split()[-1] if img.mode == 'RGBA' else None)
                img = background
            elif img.mode != 'RGB':
                img = img.convert('RGB')
            
            # 转换为JPG字节流
            jpg_buffer = io.BytesIO()
            img.save(jpg_buffer, format='JPEG', quality=95, optimize=True)
            jpg_buffer.seek(0)
            
            # 生成存储路径
            object_path = f"{id}/{file_name}"
            bucket_name = "avatar" if task_type == "avatar" else "video-cover"
            
            # 上传到MinIO（直接使用字节流）
            minio_client.put_object(
                bucket_name,
                object_path,
                jpg_buffer,
                length=jpg_buffer.getbuffer().nbytes,
                content_type='image/jpeg'
            )
            
            return object_path
            
    except Exception as e:
        raise RuntimeError(f"Image processing failed: {str(e)}")

def async_image_processing(token, id, file_name, task):
    """异步处理图片并发送回调"""
    local_path = f"/tmp/{file_name}"
    callback_data = {"sessionToken": token}
    
    try:
        # 从MinIO下载文件
        minio_client.fget_object("temp", file_name, local_path)
        
        # 处理图片
        image_path = process_image(id, local_path, file_name, task)
        callback_data["status"] = "success"
        callback_data["objectName"] = image_path
        
    except S3Error as e:
        callback_data["status"] = "failure"
        callback_data["objectName"] = f"Storage error: {str(e)}"
    except Exception as e:
        callback_data["status"] = "failure"
        callback_data["objectName"] = str(e)
    finally:
        # 清理临时文件
        if os.path.exists(local_path):
            os.remove(local_path)
        # 删除MinIO中的原始文件
        try:
            minio_client.remove_object("temp", file_name)
        except S3Error as e:
            print(f"Failed to remove temp object: {str(e)}\n")
    
    # 根据任务类型选择回调API
    callback_api = CALLBACK_AVATAR_API if task == "avatar" else CALLBACK_COVER_API
    api_name = CALLBACK_AVATAR_API_NAME if task == "avatar" else CALLBACK_COVER_API_NAME
    
    # 发送回调通知
    questPost(callback_data, callback_api, api_name)

image_bp = Blueprint('image_bp', __name__)

@image_bp.route('/image', methods=['POST'])
def handle_image():
    # 获取请求参数
    # print(request.get_data(as_text=True))
    print(f"input json: {request.json}\n")
    token = request.json.get('token')
    id = request.json.get('id')
    file_name = request.json.get('file_name')
    task = request.json.get('task')

    print(f"token: {token}, id: {id}, file_name: {file_name}, task: {task}\n")
    
    # 验证参数
    if not all([token, id, file_name, task]):
        print("Missing parameters: token, id, file_name and task are required\n")
        return jsonify({
            "status": "failure",
            "message": "Missing parameters: token, id, file_name and task are required"
        }), 400
        
    if task not in ["avatar", "cover"]:
        print("Invalid task type. Must be 'avatar' or 'cover'\n")
        return jsonify({
            "status": "failure",
            "message": "Invalid task type. Must be 'avatar' or 'cover'"
        }), 400
    
    # 启动异步处理线程
    threading.Thread(
        target=async_image_processing,
        args=(token, id, file_name, task)
    ).start()
    
    # 立即返回接受响应
    return jsonify({"status": "success"}), 200



