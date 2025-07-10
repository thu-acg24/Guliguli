import React, { useState, useRef } from "react";
import HlsVideoPlayerWrapper from "./HlsVideoPlayerWrapper";
import "./VideoPlayerSection.css";
import { Video } from 'Plugins/VideoService/Objects/Video';
import { formatTime, formatCount } from 'Components/Formatter';
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus"
import DanmakuInput from './DanmakuInput';
import Danmaku from 'danmaku';
import { LikeIcon, FavoriteIcon, ReportIcon } from 'Images/Icons';

const VideoPlayerSection: React.FC<{
  video_id: string;
  videoInfo: Video;
  isLiked: boolean;
  isFavorited: boolean;
  likeVideo: () => void;
  favoriteVideo: () => void;
  isLoggedIn: boolean;
  setShowLoginModal: (value: boolean) => void;
}> = ({
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
  const [isDescriptionExpanded, setIsDescriptionExpanded] = useState(false);
  const danmakuRef = useRef<Danmaku | null>(null);

  const toggleDescription = () => {
    setIsDescriptionExpanded(!isDescriptionExpanded);
  };

  return (
    <div className="video-video-player-section">
      <h1 className="video-video-title">{videoInfo.title}</h1>

      <div className="video-video-meta">
        <span>播放: {formatCount(videoInfo.views)}</span>
        <span>投稿时间: {formatTime(videoInfo.uploadTime, false)}</span>
      </div>

      <div className="video-video-player-container">
        <HlsVideoPlayerWrapper
          videoID={Number(video_id)}
          videoInfo={videoInfo}
          onTimeUpdate={setCurrentTime}
          danmakuRef={danmakuRef}
        />
      </div>
      
      {videoInfo.status === VideoStatus.approved && (
        <DanmakuInput
          videoID={Number(video_id)}
          isLoggedIn={isLoggedIn}
          setShowLoginModal={setShowLoginModal}
          currentTime={currentTime}
          danmakuRef={danmakuRef}
        />
      )}

      {videoInfo.status === VideoStatus.approved && (
        <div className="video-video-actions">
          <div
            className={`video-action-container ${isLiked ? 'liked' : ''}`}
            onClick={() => isLoggedIn ? likeVideo() : setShowLoginModal(true)}
          >
            <LikeIcon className="video-icon" />
            <span>{formatCount(videoInfo.likes)}</span>
          </div>
          <div
            className={`video-action-container ${isFavorited ? 'favorited' : ''}`}
            onClick={() => isLoggedIn ? favoriteVideo() : setShowLoginModal(true)}
          >
            <FavoriteIcon className="video-icon" />
            <span>{formatCount(videoInfo.favorites)}</span>
          </div>
          <div 
            className="video-report-container"
            // onClick={() => isLoggedIn ? favoriteVideo() : setShowLoginModal(true)}
          >
            <ReportIcon className="video-icon" />
            <span>举报</span>
          </div>
        </div>
      )}

      {/* 视频描述区域 */}
      {videoInfo.description && (
        <div className="video-description-container">
          <div 
            className={`video-description ${isDescriptionExpanded ? 'expanded' : ''}`}
          >
            {videoInfo.description}
          </div>
          {videoInfo.description.split('\n').length > 5 && (
            <button 
              className="video-description-toggle"
              onClick={toggleDescription}
            >
              {isDescriptionExpanded ? '收起' : '展开更多'}
            </button>
          )}
        </div>
      )}

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