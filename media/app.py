from flask import Flask
from common import create_buckets
import image_processor
import video_processor

app = Flask(__name__)

# 或者直接导入路由函数
app.add_url_rule('/image', view_func=image_processor.handle_image, methods=['POST'])
app.add_url_rule('/video', view_func=video_processor.handle_video, methods=['POST'])

if __name__ == '__main__':
    create_buckets()
    app.run(host='0.0.0.0', port=5000)