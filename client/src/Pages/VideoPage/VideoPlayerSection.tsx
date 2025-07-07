import React from "react";
import HlsVideoPlayerWrapper from "./HlsVideoPlayerWrapper";
import "./VideoPage.css";
import { Video } from 'Plugins/VideoService/Objects/Video';

interface VideoPlayerSectionProps {
  video_id: string;
  videoinfo: Video;
  isLiked: boolean;
  isFavorited: boolean;
  likeVideo: () => void;
  favoriteVideo: () => void;
}

const VideoPlayerSection: React.FC<VideoPlayerSectionProps> = ({
  video_id,
  videoinfo,
  isLiked,
  isFavorited,
  likeVideo,
  favoriteVideo,
}) => {
  return (
    <div className="video-video-player-section">
      <h1 className="video-video-title">{videoinfo.title}</h1>

      <div className="video-video-meta">
        <span>播放: {videoinfo.views}</span>
        <span>投稿时间: {videoinfo.uploadTime}</span>
      </div>

      <div className="video-video-player-container">
        <HlsVideoPlayerWrapper videoID={Number(video_id)} />
      </div>

      <div className="video-video-actions">
        <button
          className={`video-videopage-action-btn ${isLiked ? 'liked' : ''}`}
          onClick={() => likeVideo()}
        >
          {isLiked ? '点赞' : '点赞'}&nbsp;{videoinfo.likes}
        </button>
        <button
          className={`video-videopage-action-btn ${isFavorited ? 'favorited' : ''}`}
          onClick={() => favoriteVideo()}
        >
          {isFavorited ? '收藏' : '收藏'}&nbsp;{videoinfo.favorites}
        </button>
      </div>

      <div className="video-video-tags">
        {videoinfo.tag?.map(ttag => (
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