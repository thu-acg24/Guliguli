from flask import request, jsonify
from minio.error import S3Error
from PIL import Image
import io
import uuid
import os

from common import minio_client

def process_image(user_id, file_path, task_type):
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
            object_path = f"{user_id}/{uuid.uuid4().hex}.jpg"
            bucket_name = "avatar" if task_type == "avatar" else "video-cover"
            
            # 上传到MinIO（直接使用字节流）
            minio_client.put_object(
                bucket_name,
                object_path,
                jpg_buffer,
                length=jpg_buffer.getbuffer().nbytes,
                content_type='image/jpeg'
            )
            
            return f"{object_path}"
            
    except Exception as e:
        raise RuntimeError(f"Image processing failed: {str(e)}")

@app.route('/image', methods=['POST'])
def handle_image():
    # 获取请求参数
    user_id = request.form.get('id')
    file_name = request.form.get('file_name')
    task = request.form.get('task')
    
    # 验证参数
    if not user_id or not file_name or not task:
        return jsonify({
            "status": "failure",
            "message": "Missing parameters: id, file_name and task are required"
        }), 400
        
    if task not in ["avatar", "cover"]:
        return jsonify({
            "status": "failure",
            "message": "Invalid task type. Must be 'avatar' or 'cover'"
        }), 400
    
    # 临时文件路径
    local_path = f"/tmp/{file_name}"
    
    try:
        # 从MinIO下载文件
        minio_client.fget_object("temp", file_name, local_path)
        
        # 处理图片
        image_path = process_image(user_id, local_path, task)
        
        # 删除临时文件
        os.remove(local_path)
        
        # 删除MinIO中的原始文件
        minio_client.remove_object("temp", file_name)
        
        return jsonify({
            "status": "success",
            "message": image_path
        }), 200
        
    except S3Error as e:
        # 清理可能残留的文件
        if os.path.exists(local_path):
            os.remove(local_path)
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