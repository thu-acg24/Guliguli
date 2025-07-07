import React, { useEffect, useRef } from 'react';
import Hls from 'hls.js';
import './HlsVideoPlayer.css';

interface MinioVideoPlayerProps {
  videoUrl: string;
}

const MinioVideoPlayer: React.FC<MinioVideoPlayerProps> = ({ videoUrl }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    // 清理之前的 HLS 实例
    if (hlsRef.current) {
      hlsRef.current.destroy();
    }

    if (Hls.isSupported()) {
      const hls = new Hls(); // 移除 xhrSetup，使用 URL 自带的签名
      hlsRef.current = hls;

      // 加载视频源
      hls.loadSource(videoUrl);
      hls.attachMedia(video);

      // 错误处理（增强日志）
      hls.on(Hls.Events.ERROR, (_, data) => {
        console.error('HLS Error:', data);
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              console.error('Network error, trying to recover');
              hls.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              console.error('Media error, recovering');
              hls.recoverMediaError();
              break;
            default:
              console.error('Unrecoverable error');
              hls.destroy();
              break;
          }
        }
      });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      // Safari 原生支持
      video.src = videoUrl;
    }

    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
      }
    };
  }, [videoUrl]);

  return (
    <div className="video-container">
      <video
        ref={videoRef}
        controls
        autoPlay
        muted
        className="video-player"
      />
    </div>
  );
};

export default MinioVideoPlayer;