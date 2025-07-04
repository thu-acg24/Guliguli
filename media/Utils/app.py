from flask import Flask
from common import create_buckets
from image_processor import image_bp
from video_processor import video_bp

def create_app():
    app = Flask(__name__)
    app.register_blueprint(image_bp)
    app.register_blueprint(video_bp)
    with app.app_context():
        print("Initializing MinIO buckets...")
        create_buckets()
    return app

app = create_app()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)