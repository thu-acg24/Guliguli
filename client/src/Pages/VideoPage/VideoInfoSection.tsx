import React from "react";
import "./VideoPage.css";
import { Video } from 'Plugins/VideoService/Objects/Video';

interface VideoInfoSectionProps {
  videoinfo: Video;
}

const VideoInfoSection: React.FC<VideoInfoSectionProps> = ({ videoinfo }) => {
  return (
    <div className="video-video-player-section">
      <h1 className="video-video-title">{videoinfo.title}</h1>
      <div className="video-video-meta">
        <span>播放: {videoinfo.views}</span>
        <span>投稿时间: {videoinfo.uploadTime}</span>
      </div>
    </div>
  );
};

export default VideoInfoSection;