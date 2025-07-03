#!/bin/bash

# 使用环境变量（由Docker注入）
PORT=${PORT:-5000}
WORKERS=${WORKERS:-4}

echo "===== System Diagnostics ====="
echo "NVIDIA GPU Status:"
nvidia-smi || echo "nvidia-smi not available"
echo ""
echo "FFmpeg Version:"
ffmpeg -version || echo "ffmpeg not available"
echo ""
echo "FFmpeg Hardware Acceleration Support:"
ffmpeg -hide_banner -hwaccels || echo "hwaccels check failed"
echo "=============================="

# 启动Gunicorn
exec gunicorn -w $WORKERS -b 0.0.0.0:$PORT app:app