import React, { useEffect, useRef, useState } from "react";
import Hls from "hls.js";

interface VideoPlayerProps {
  videoUrl: string;
  posterUrl?: string;
}

const VideoPlayer: React.FC<VideoPlayerProps> = ({ videoUrl, posterUrl }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [volume, setVolume] = useState(0.7);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [isMuted, setIsMuted] = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1.0);
  const [error, setError] = useState<string | null>(null);
  const [hlsInstance, setHlsInstance] = useState<Hls | null>(null);

  // 初始化HLS播放器
  useEffect(() => {
    if (!videoRef.current) return;

    const video = videoRef.current;
    let hls: Hls | null = null;

    if (Hls.isSupported()) {
      hls = new Hls({
        maxBufferLength: 30,
        maxBufferSize: 60 * 1000 * 1000, // 60MB
        enableWorker: true,
      });
      hls.loadSource(videoUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        console.log("Manifest loaded");
        setDuration(video.duration);
      });

      hls.on(Hls.Events.ERROR, (event, data) => {
        console.error("HLS error:", data);
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              hls?.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              hls?.recoverMediaError();
              break;
            default:
              setError("无法播放视频，请刷新页面重试");
              break;
          }
        }
      });
      setHlsInstance(hls);
    } else if (video.canPlayType("application/vnd.apple.mpegurl")) {
      // 对于Safari浏览器（原生支持HLS）
      video.src = videoUrl;
      video.addEventListener("loadedmetadata", () => {
        setDuration(video.duration);
      });
    } else {
      setError("您的浏览器不支持视频播放");
    }

    return () => {
      if (hls) {
        hls.destroy();
        setHlsInstance(null);
      }
    };
  }, [videoUrl]);

  // 播放器控制函数
  const togglePlay = () => {
    if (!videoRef.current) return;

    if (videoRef.current.paused) {
      videoRef.current.play().then(() => {
        setIsPlaying(true);
      }).catch((e) => {
        console.error("播放失败:", e);
        setError("播放失败: " + e.message);
      });
    } else {
      videoRef.current.pause();
      setIsPlaying(false);
    }
  };

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!videoRef.current) return;

    const time = parseFloat(e.target.value);
    videoRef.current.currentTime = time;
    setCurrentTime(time);
  };

  const handleVolumeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!videoRef.current) return;

    const vol = parseFloat(e.target.value);
    videoRef.current.volume = vol;
    setVolume(vol);
    setIsMuted(vol === 0);
  };

  const toggleMute = () => {
    if (!videoRef.current) return;

    videoRef.current.muted = !videoRef.current.muted;
    setIsMuted(videoRef.current.muted);
    if (!videoRef.current.muted && volume === 0) {
      setVolume(0.5);
      videoRef.current.volume = 0.5;
    }
  };

  const toggleFullscreen = () => {
    if (!videoRef.current) return;

    if (!document.fullscreenElement) {
      videoRef.current.requestFullscreen().catch((err) => {
        console.error(`全屏请求失败: ${err.message}`);
      });
      setIsFullscreen(true);
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
        setIsFullscreen(false);
      }
    }
  };

  const changePlaybackRate = (rate: number) => {
    if (!videoRef.current) return;

    videoRef.current.playbackRate = rate;
    setPlaybackRate(rate);
  };

  // 更新当前播放时间
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const updateTime = () => setCurrentTime(video.currentTime);
    video.addEventListener("timeupdate", updateTime);

    return () => {
      video.removeEventListener("timeupdate", updateTime);
    };
  }, []);

  // 处理视频结束
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const handleEnded = () => {
      setIsPlaying(false);
    };

    video.addEventListener("ended", handleEnded);
    return () => {
      video.removeEventListener("ended", handleEnded);
    };
  }, []);

  return (
    <div className="video-player-wrapper">
      {error ? (
        <div className="video-player-error">
          <p>{error}</p>
          <button onClick={() => window.location.reload()}>刷新重试</button>
        </div>
      ) : (
        <>
          <video
            ref={videoRef}
            className="video-player"
            onClick={togglePlay}
            poster={posterUrl}
          />
          <div className="video-player-controls">
            <button onClick={togglePlay} className="video-play-pause">
              {isPlaying ? "❚❚" : "▶"}
            </button>

            <input
              type="range"
              min="0"
              max={duration || 100}
              value={currentTime}
              onChange={handleSeek}
              className="video-seek-bar"
            />

            <span className="video-time">
              {formatTime(currentTime)} / {formatTime(duration)}
            </span>

            <button onClick={toggleMute} className="video-volume-btn">
              {isMuted ? "🔇" : volume > 0.5 ? "🔊" : "🔈"}
            </button>

            <input
              type="range"
              min="0"
              max="1"
              step="0.01"
              value={volume}
              onChange={handleVolumeChange}
              className="video-volume-bar"
            />

            <div className="video-playback-rate">
              <select
                value={playbackRate}
                onChange={(e) => changePlaybackRate(parseFloat(e.target.value))}
              >
                <option value="0.5">0.5x</option>
                <option value="0.75">0.75x</option>
                <option value="1.0">1.0x</option>
                <option value="1.25">1.25x</option>
                <option value="1.5">1.5x</option>
                <option value="2.0">2.0x</option>
              </select>
            </div>

            <button onClick={toggleFullscreen} className="video-fullscreen-btn">
              {isFullscreen ? "⤢" : "⤡"}
            </button>
          </div>
        </>
      )}
    </div>
  );
};

// 辅助函数：格式化时间（秒 -> mm:ss）
function formatTime(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = Math.floor(seconds % 60);
  return `${minutes}:${remainingSeconds < 10 ? "0" : ""}${remainingSeconds}`;
}

export default VideoPlayer;