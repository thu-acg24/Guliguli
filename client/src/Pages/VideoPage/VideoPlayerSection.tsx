import React, { useState, useRef } from "react";
import HlsVideoPlayerWrapper from "./HlsVideoPlayerWrapper";
import "./VideoPlayerSection.css";
import { Video } from 'Plugins/VideoService/Objects/Video';
import { formatTime, formatCount } from 'Components/Formatter';
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus"
import DanmakuInput from './DanmakuInput';
import Danmaku from 'danmaku';
import { VideoReportModal } from "./ReportComponents";
import { LikeIcon, FavoriteIcon, ReportIcon,PlayCountIcon } from 'Images/Icons';
import { useNavigateSearch } from "Globals/Navigate";

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
  const [showVideoReport, setShowVideoReport] = useState(false);
  const toggleDescription = () => {
    setIsDescriptionExpanded(!isDescriptionExpanded);
  };
  const { navigateSearch } = useNavigateSearch();

  const handleTagClick = (tag: string) => {
    navigateSearch(tag);
  }

  return (
    <div className="video-video-player-section">
      <div className="video-video-title">{videoInfo.title}</div>

      <div className="video-video-meta">
        
        <div className="video-icon-meta">
          <PlayCountIcon className="video-playcount-icon" />
          {formatCount(videoInfo.views) || 0}
        </div>
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
            onClick={() => isLoggedIn ? setShowVideoReport(true) : setShowLoginModal(true)}
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
            onClick={() => handleTagClick(ttag)}
          >
            {ttag}
          </span>
        ))}
      </div>
    {showVideoReport && (
      <VideoReportModal
        visible={showVideoReport}
        onCancel={() => setShowVideoReport(false)}
        videoID={Number(video_id)}
      />
    )}
    </div>
  );
};

export default VideoPlayerSection;