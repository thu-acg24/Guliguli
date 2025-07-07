import React from 'react';

interface RecommendedVideo {
  id: string;
  title: string;
  author: string;
  views: string;
  danmaku: string;
  cover: string;
}

interface RecommendedVideosProps {
  videos: RecommendedVideo[];
}

const RecommendedVideos: React.FC<RecommendedVideosProps> = ({ videos }) => {
  return (
    <div className="video-recommended-videos">
      <h3 className="video-recommended-title">推荐视频</h3>
      {videos.map(video => (
        <div key={video.id} className="video-recommended-video">
          <div
            className="video-recommended-cover"
            onClick={() => alert(`跳转到视频 ${video.id}`)}
          >
            <img src={video.cover} alt="视频封面" />
          </div>
          <div className="video-recommended-info">
            <div
              className="video-recommended-title"
              onClick={() => alert(`跳转到视频 ${video.id}`)}
            >
              {video.title}
            </div>
            <div
              className="video-recommended-author"
              onClick={() => alert(`跳转到UP主 ${video.author}`)}
            >
              {video.author}
            </div>
            <div className="video-recommended-meta">
              <span>播放: {video.views}</span>
              <span>弹幕: {video.danmaku}</span>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
};

export default RecommendedVideos;