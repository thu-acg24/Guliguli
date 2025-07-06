import React, { useEffect, useRef } from 'react';
import Hls, { 
  HlsConfig, 
  Events, 
  ErrorTypes, 
  ErrorDetails,
  Fragment,
  LevelDetails
} from 'hls.js';

interface HlsVideoPlayerProps {
  videoPath: string;
  tsPrefix: string;
  sliceCount: number;
  className?: string;
  autoPlay?: boolean;
  controls?: boolean;
  poster?: string;
  serverBaseUrl?: string;
  hlsConfig?: Partial<HlsConfig>; // 允许自定义HLS配置
}

// 扩展默认的Loader类型
declare module 'hls.js' {
  interface Loader<T extends LoaderContext> {
    getLoaderType(levelDetails: LevelDetails): void;
  }
}

const HlsVideoPlayer: React.FC<HlsVideoPlayerProps> = ({
  videoPath,
  tsPrefix,
  sliceCount,
  className = '',
  autoPlay = true,
  controls = true,
  poster = '',
  serverBaseUrl = 'http://你的服务器地址/',
  hlsConfig = {}
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);

  const buildHlsUrl = () => {
    return `${serverBaseUrl}${videoPath}/index.m3u8`;
  };

  const configureSegmentLoader = (hls: Hls) => {
    const OriginalLoader = hls.config.loader;
    
    class CustomLoader extends OriginalLoader {
      constructor(config: HlsConfig) {
        super(config);
      }
      
      getLoaderType(levelDetails: LevelDetails) {
        if (levelDetails?.fragments) {
          levelDetails.fragments.forEach((frag: Fragment) => {
            if (frag.relurl && frag.sn !== undefined) {
              frag.relurl = `${tsPrefix}_${frag.sn}.ts`;
            }
          });
        }
        // @ts-ignore - 调用父类方法
        return super.getLoaderType(levelDetails);
      }
    }

    hls.config.loader = CustomLoader as typeof OriginalLoader;
  };

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !videoPath || !tsPrefix) return;

    const initHls = () => {
      if (Hls.isSupported()) {
        const config: HlsConfig = {
          maxBufferLength: 30,
          maxMaxBufferLength: 600,
          enableWorker: true,
          maxBufferSize: 60 * 1000 * 1000,
          maxBufferHole: 0.5,
          ...hlsConfig // 合并自定义配置
        };

        const hls = new Hls(config);
        configureSegmentLoader(hls);
        
        hlsRef.current = hls;
        hls.loadSource(buildHlsUrl());
        hls.attachMedia(video);

        hls.on(Events.MANIFEST_PARSED, () => {
          if (autoPlay) {
            video.play().catch(e => console.error('自动播放失败:', e));
          }
        });

        hls.on(Events.ERROR, (event, data) => {
          if (data.fatal) {
            switch (data.type) {
              case ErrorTypes.NETWORK_ERROR:
                console.error('网络错误，尝试恢复');
                hls.startLoad();
                break;
              case ErrorTypes.MEDIA_ERROR:
                console.error('媒体错误，尝试恢复');
                hls.recoverMediaError();
                break;
              default:
                console.error('无法恢复的错误:', data.details);
                initHls();
                break;
            }
          }
        });
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = buildHlsUrl();
        video.addEventListener('loadedmetadata', () => {
          if (autoPlay) {
            video.play().catch(e => console.error('自动播放失败:', e));
          }
        });
      }
    };

    initHls();

    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
      }
    };
  }, [videoPath, tsPrefix, sliceCount, autoPlay, serverBaseUrl, hlsConfig]);

  return (
    <video
      ref={videoRef}
      className={className}
      controls={controls}
      poster={poster}
      playsInline
      preload="auto"
    />
  );
};

export default HlsVideoPlayer;