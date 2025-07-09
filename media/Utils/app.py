from flask import Flask, request, jsonify
from common import create_buckets
from image_processor import image_bp
from video_processor import video_bp
import logging
from werkzeug.middleware.proxy_fix import ProxyFix

def create_app():
    app = Flask(__name__)
    app.logger.setLevel(logging.INFO)
    image_bp.logger = app.logger
    video_bp.logger = app.logger
    app.register_blueprint(image_bp)
    app.register_blueprint(video_bp)
    with app.app_context():
        print("Initializing MinIO buckets...")
        create_buckets()
    return app

app = create_app()

@app.route('/debug')
def debug_headers():
    headers = {k: v for k, v in request.headers.items()}
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
    headers['REMOTE_ADDR'] = target_ip
    return jsonify(headers)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)