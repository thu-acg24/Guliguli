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
import logging

from common import minio_client, CALLBACK_AVATAR_API_NAME, CALLBACK_COVER_API_NAME

image_bp = Blueprint('image_bp', __name__)

def questPost(data: dict, api_name: str, target_ip: str, target_port: int):
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

    callback_api = f"http://{target_ip}:{target_port}/api/{api_name}"
    
    try:
        image_bp.logger.info(f"callback: url={callback_api}, headers={headers}, data={body_str}")
        response = requests.post(
            url=callback_api,
            headers=headers,
            data=body_str,
            timeout=10
        )
        image_bp.logger.info(f"Callback Status: {response.status_code}, Response: {response.text}\n")
    except requests.exceptions.RequestException as e:
        image_bp.logger.info(f"Callback failed: {str(e)}\n")

def process_image(id, file_path, file_name, task_type):
    """处理图片文件并转换为JPG格式"""
    try:
        # 读取并验证原始图片
        with Image.open(file_path) as img:
            # 验证图片尺寸（最大2048x2048）
            # if img.width > 2048 or img.height > 2048:
            if img.width * img.height > 2048 * 2048 * 2:
                raise ValueError("Image dimensions too large (max 2048x2048x2)")
            
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

def async_image_processing(token, id, file_name, task, target_ip):
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
            image_bp.logger.info(f"Failed to remove temp object: {str(e)}\n")
    
    # 根据任务类型选择回调API
    api_name = CALLBACK_AVATAR_API_NAME if task == "avatar" else CALLBACK_COVER_API_NAME
    
    # 发送回调通知
    questPost(callback_data, api_name, target_ip, 10012 if task == "avatar" else 10016)

@image_bp.route('/image', methods=['POST'])
def handle_image():
    # 获取请求参数
    # print(request.get_data(as_text=True))
    image_bp.logger.info(f"input json: {request.json}\n")
    token = request.json.get('token')
    id = request.json.get('id')
    file_name = request.json.get('file_name')
    task = request.json.get('task')
    # target_ip = request.headers.get('X-Real-IP', request.remote_addr)
    for key, value in request.headers.items():
        image_bp.logger.info(f"{key}: {value}")
    forwarded_for = request.headers.get('X-Forwarded-For')
    if forwarded_for:
        # 获取第一个 IP 地址
        client_ip = forwarded_for.split(',')[0].strip()
        image_bp.logger.info(f"Your client IP address is: {client_ip}")
        target_ip = client_ip
    else:
        # 如果没有 X-Forwarded-For 头部，尝试获取 X-Real-IP
        real_ip = request.headers.get('X-Real-IP')
        if real_ip:
            target_ip = real_ip
            image_bp.logger.info(f"Your real IP address is: {real_ip}")
        else:
            # 如果都没有，返回默认值
            return jsonify({"status": "failure", "message": "Unable to determine your IP address."}), 400

    image_bp.logger.info(f"token: {token}, id: {id}, file_name: {file_name}, task: {task}, target_ip: {target_ip}\n")
    
    # 验证参数
    if not all([token, id, file_name, task]):
        image_bp.logger.info("Missing parameters: token, id, file_name and task are required\n")
        return jsonify({
            "status": "failure",
            "message": "Missing parameters: token, id, file_name and task are required"
        }), 400
        
    if task not in ["avatar", "cover"]:
        image_bp.logger.info("Invalid task type. Must be 'avatar' or 'cover'\n")
        return jsonify({
            "status": "failure",
            "message": "Invalid task type. Must be 'avatar' or 'cover'"
        }), 400
    
    # 启动异步处理线程
    threading.Thread(
        target=async_image_processing,
        args=(token, id, file_name, task, target_ip)
    ).start()
    
    # 立即返回接受响应
    return jsonify({"status": "success"}), 200



