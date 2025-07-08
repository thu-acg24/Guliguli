// Update VideoPlayerSection.tsx
import React,{ useState, useRef } from "react";
import HlsVideoPlayerWrapper from "./HlsVideoPlayerWrapper";
import "./VideoPage.css";
import { Video } from 'Plugins/VideoService/Objects/Video';
import { formatTime } from 'Components/GetTime';
import DanmakuInput from './DanmakuInput';
import Danmaku from 'danmaku';

interface VideoPlayerSectionProps {
  video_id: string;
  videoInfo: Video;
  isLiked: boolean;
  isFavorited: boolean;
  likeVideo: () => void;
  favoriteVideo: () => void;
  isLoggedIn: boolean;
  setShowLoginModal: (value: boolean) => void;
}

const VideoPlayerSection: React.FC<VideoPlayerSectionProps> = ({
  video_id,
  videoInfo,
  isLiked,
  isFavorited,
  likeVideo,
  favoriteVideo,
  isLoggedIn,
  setShowLoginModal,
}) => {
  const [currentTime, setCurrentTime] = useState(0);
  const danmakuRef = useRef<Danmaku | null>(null);
  return (
    <div className="video-video-player-section">
      <h1 className="video-video-title">{videoInfo.title}</h1>

      <div className="video-video-meta">
        <span>播放: {videoInfo.views}</span>
        <span>投稿时间: {formatTime(videoInfo.uploadTime,false)}</span>
      </div>

      <div className="video-video-player-container">
        <HlsVideoPlayerWrapper 
          videoID={Number(video_id)} 
          videoInfo={videoInfo} 
          onTimeUpdate={setCurrentTime}
          danmakuRef={danmakuRef}
        />
      </div>

      <DanmakuInput 
        videoID={Number(video_id)} 
        isLoggedIn={isLoggedIn} 
        setShowLoginModal={setShowLoginModal}
        currentTime={currentTime}
        danmakuRef={danmakuRef}
      />

      <div className="video-video-actions">
        <button
          className={`video-videopage-action-btn ${isLiked ? 'liked' : ''}`}
          onClick={() => likeVideo()}
        >
          {isLiked ? '点赞' : '点赞'}&nbsp;{videoInfo.likes}
        </button>
        <button
          className={`video-videopage-action-btn ${isFavorited ? 'favorited' : ''}`}
          onClick={() => favoriteVideo()}
        >
          {isFavorited ? '收藏' : '收藏'}&nbsp;{videoInfo.favorites}
        </button>
      </div>

      <div className="video-video-tags">
        {videoInfo.tag?.map(ttag => (
          <span
            key={ttag}
            className="video-tag"
            onClick={() => alert(`搜索标签: ${ttag}`)}
          >
            {ttag}
          </span>
        ))}
      </div>
    </div>
  );
};

export default VideoPlayerSection;