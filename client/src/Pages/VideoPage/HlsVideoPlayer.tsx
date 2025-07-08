import React, { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import Danmaku from 'danmaku';
import { Danmaku as DanmakuObj } from 'Plugins/DanmakuService/Objects/Danmaku'
import './HlsVideoPlayer.css';
import { Video } from 'Plugins/VideoService/Objects/Video';

interface MinioVideoPlayerProps {
  videoUrl: string;
  videoInfo: Video;
  danmakuList: DanmakuObj[];
}

const MinioVideoPlayer: React.FC<MinioVideoPlayerProps> = ({ videoUrl, videoInfo, danmakuList }) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const danmakuRef = useRef<Danmaku | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [volume, setVolume] = useState(0.7);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const [showControls, setShowControls] = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1);
  const progressContainerRef = useRef<HTMLDivElement>(null);
  const controlsTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const lastClickTimeRef = useRef(0);
  const togglePlayTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const [isRateMenuOpen, setIsRateMenuOpen] = useState(false);
  const playbackRates = [0.5, 0.75, 1, 1.25, 1.5, 2];

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    video.volume = volume;
    video.playbackRate = playbackRate;

    if (hlsRef.current) {
      hlsRef.current.destroy();
    }
    if (danmakuRef.current) {
      danmakuRef.current.destroy();
    }

    if (Hls.isSupported()) {
      const hls = new Hls();
      hlsRef.current = hls;

      hls.loadSource(videoUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        setDuration(video.duration);
        video.play().catch(e => console.log('Auto-play prevented:', e));
      });

      hls.on(Hls.Events.ERROR, (_, data) => {
        console.error('HLS Error:', data);
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              hls.startLoad();
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              hls.recoverMediaError();
              break;
            default:
              hls.destroy();
              break;
          }
        }
      });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = videoUrl;
      video.addEventListener('loadedmetadata', () => {
        setDuration(video.duration);
        video.play().catch(e => console.log('Auto-play prevented:', e));
      });
    }

    const playListener = () => setIsPlaying(true);
    const pauseListener = () => setIsPlaying(false);
    const volumeChangeListener = () => setVolume(video.volume);
    const timeUpdateListener = () => setCurrentTime(video.currentTime);
    const durationChangeListener = () => setDuration(video.duration);

    video.addEventListener('play', playListener);
    video.addEventListener('pause', pauseListener);
    video.addEventListener('volumechange', volumeChangeListener);
    video.addEventListener('timeupdate', timeUpdateListener);
    video.addEventListener('durationchange', durationChangeListener);

    const fullscreenChangeListener = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener('fullscreenchange', fullscreenChangeListener);

    const container = containerRef.current;
    if (container) {
      const handleMouseMove = () => {
        setShowControls(true);
        resetControlsTimeout();
      };

      const handleMouseLeave = () => {
        setShowControls(false);
        clearTimeout(controlsTimeoutRef.current);
      };

      container.addEventListener('mousemove', handleMouseMove);
      container.addEventListener('mouseleave', handleMouseLeave);


      const danmaku = new Danmaku({
        container: container,
        media: video,
        engine: "canvas",
        comments: []
      });
      danmakuList.forEach((d) => {
        danmaku.emit({
          text: d.content,
          time: d.timeInVideo,
          style: {
            font: '20px sans-serif',
            textAlign: 'start',
            fillStyle: d.danmakuColor,
            strokeStyle: d.danmakuColor,
            lineWidth: 1,
          }
        })
      })
      danmakuRef.current = danmaku;
      container.addEventListener('resize', () => {danmaku.resize()});

      return () => {
        if (hlsRef.current) {
          hlsRef.current.destroy();
        }
        if (danmakuRef.current) {
          danmakuRef.current.destroy();
        }
        video.removeEventListener('play', playListener);
        video.removeEventListener('pause', pauseListener);
        video.removeEventListener('volumechange', volumeChangeListener);
        video.removeEventListener('timeupdate', timeUpdateListener);
        video.removeEventListener('durationchange', durationChangeListener);
        document.removeEventListener('fullscreenchange', fullscreenChangeListener);
        container.removeEventListener('mousemove', handleMouseMove);
        container.removeEventListener('mouseleave', handleMouseLeave);
        clearTimeout(controlsTimeoutRef.current);
      };
    }
  }, [videoUrl]);

  const resetControlsTimeout = () => {
    clearTimeout(controlsTimeoutRef.current);
    controlsTimeoutRef.current = setTimeout(() => {
      setShowControls(false);
    }, 3000);
  };

  const togglePlay = () => {
    const video = videoRef.current;
    if (!video) return;
    
    if (video.paused) {
      video.play().catch(e => console.log('Play failed:', e));
      resetControlsTimeout();
    } else {
      video.pause();
      setShowControls(true);
      clearTimeout(controlsTimeoutRef.current);
    }
  };

  const handleVideoClick = (e: React.MouseEvent) => {
    const now = Date.now();
    const lastClickTime = lastClickTimeRef.current;

    if (now - lastClickTime < 200) {
      if (togglePlayTimeoutRef.current) {
        clearTimeout(togglePlayTimeoutRef.current);
        togglePlayTimeoutRef.current = null;
      }
      toggleFullscreen();
      lastClickTimeRef.current = 0;
      return;
    }

    lastClickTimeRef.current = now;

    if (togglePlayTimeoutRef.current) {
      clearTimeout(togglePlayTimeoutRef.current);
    }

    togglePlayTimeoutRef.current = setTimeout(() => {
      togglePlay();
      togglePlayTimeoutRef.current = null;
    }, 300);
  };

  const toggleFullscreen = () => {
    const container = containerRef.current;
    if (!container) return;

    if (!isFullscreen) {
      if (container.requestFullscreen) {
        container.requestFullscreen();
      }
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
      }
    }
  };

  const handleProgressClick = (e: React.MouseEvent<HTMLDivElement>) => {
    const video = videoRef.current;
    const progressContainer = progressContainerRef.current;
    if (!video || !progressContainer) return;

    const rect = progressContainer.getBoundingClientRect();
    const pos = (e.clientX - rect.left) / rect.width;
    video.currentTime = pos * duration;
    resetControlsTimeout();
  };

  const changePlaybackRate = (rate: number) => {
    const video = videoRef.current;
    if (!video) return;
    
    video.playbackRate = rate;
    setPlaybackRate(rate);
    resetControlsTimeout();
  };

  const formatTime = (time: number) => {
    const minutes = Math.floor(time / 60);
    const seconds = Math.floor(time % 60);
    return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
  };

  return (
    <div 
      ref={containerRef}
      className="video-container"
      onClick={handleVideoClick}
    >
      {videoInfo?.title && (
        <div className={`fullscreen-title ${showControls ? 'visible' : ''}`}>
          {videoInfo.title}
        </div>
      )}
      
      <video
        ref={videoRef}
        controls={false}
        autoPlay
        className="video-player"
      />
      
      <div 
        className={`custom-controls ${showControls ? 'visible' : ''}`}
        onClick={(e) => e.stopPropagation()}
      >
        <button 
          onClick={(e) => { e.stopPropagation(); togglePlay(); }} 
          className="control-button"
        >
          {isPlaying ? '❚❚' : '▶'}
        </button>
        
        <input 
          type="range" 
          min="0" 
          max="1" 
          step="0.01" 
          value={volume}
          onChange={(e) => {
            e.stopPropagation();
            const newVolume = parseFloat(e.target.value);
            setVolume(newVolume);
            if (videoRef.current) videoRef.current.volume = newVolume;
          }}
          onClick={(e) => e.stopPropagation()}
          className="volume-slider"
        />
        
        <div className="time-display">
          {formatTime(currentTime)} / {formatTime(duration)}
        </div>
        
        <div 
          ref={progressContainerRef}
          className="progress-container"
          onClick={(e) => {
            e.stopPropagation();
            handleProgressClick(e);
          }}
        >
          <div 
            className="progress-bar" 
            style={{ width: `${(currentTime / duration) * 100}%` }}
          />
        </div>
        
        <div className="playback-rate-container">
          <div className="playback-rate-selector">
            <div className="playback-rate-custom-select">
              <div 
                className="selected-rate" 
                onClick={() => setIsRateMenuOpen(!isRateMenuOpen)}
              >
                {playbackRate}x
              </div>
              
              {isRateMenuOpen && (
                <div className="rate-options">
                  {playbackRates.map(rate => (
                    <div
                      key={rate}
                      className={`rate-option ${rate === playbackRate ? 'selected' : ''}`}
                      onClick={() => {
                        changePlaybackRate(rate);
                        setIsRateMenuOpen(false);
                      }}
                    >
                      {rate}x
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
        
        <button 
          onClick={(e) => {
            e.stopPropagation();
            toggleFullscreen();
          }} 
          className="control-button"
        >
          {isFullscreen ? '⤢' : '⤡'}
        </button>
      </div>
    </div>
  );
};

export default MinioVideoPlayer;