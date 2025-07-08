import React from "react";
import "./VideoPage.css";
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { Video } from 'Plugins/VideoService/Objects/Video';
import { UserStat } from 'Plugins/UserService/Objects/UserStat';
import { useNavigate } from "react-router-dom";
import DefaultCover from "Images/DefaultCover.jpg";
import { SimpleVideo } from "Components/RecommendVideoService";


interface SidebarSectionProps {
  uploaderInfo: UserInfo;
  videoInfo: Video;
  userToken: string;
  userInfo: UserInfo;
  isFollowing: boolean;
  upstat: UserStat;
  followUp: (id: number) => void;
  navigateToUser: (id: number) => void;
  recommendedVideos: SimpleVideo[];
}

const SidebarSection: React.FC<SidebarSectionProps> = ({
  uploaderInfo,
  videoInfo,
  userToken,
  userInfo,
  isFollowing,
  upstat,
  followUp,
  navigateToUser,
  recommendedVideos,
}) => {
  const navigate = useNavigate();

  const handleVideoClick = (videoId: number) => {
    if (videoId > 0) {
      navigate(`/video/${videoId}`);
    }
  };

  const handleAuthorClick = (userId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    if (userId > 0) {
      navigateToUser(userId);
    }
  };

  return (
    <div className="video-video-sidebar">
      <div className="video-up-info">
        <div className="video-up-top-row">
          <div className="video-up-avatar">
            <img 
              src={uploaderInfo.avatarPath} 
              alt="UP主头像" 
              onClick={() => navigateToUser(uploaderInfo.userID)}
              onError={(e) => { e.currentTarget.src = DefaultCover; }}
            />
          </div>
          <div className="video-up-details">
            <div className="video-up-name">
              <span onClick={() => navigateToUser(uploaderInfo.userID)}>
                {uploaderInfo.username}
              </span>
            </div>
            <div className="video-up-description" title={uploaderInfo.bio}>
              {uploaderInfo.bio?.length > 17
                ? `${uploaderInfo.bio.substring(0, 17)}...`
                : uploaderInfo.bio || "暂无简介"}
            </div>
          </div>
        </div>
        {((!userToken) || (videoInfo.uploaderID !== userInfo?.userID)) && (
          <button
            className={`video-follow-btn ${isFollowing ? 'following' : ''}`}
            onClick={() => followUp(uploaderInfo.userID)}
          >
            {isFollowing ? '已关注' : '关注'}&nbsp;{upstat.followerCount}
          </button>
        )}
      </div>

      <div className="video-recommended-videos">
        <h3 className="video-recommended-title">推荐视频</h3>
        {recommendedVideos.map(video => (
          <div key={video.videoID} className="video-recommended-video">
            <div
              className="video-recommended-cover"
              onClick={() => handleVideoClick(video.videoID)}
            >
              <img 
                src={video.cover || DefaultCover} 
                alt="视频封面" 
                onError={(e) => { e.currentTarget.src = DefaultCover; }}
              />
            </div>
            <div className="video-recommended-info">
              <div
                className="video-recommended-title"
                onClick={() => handleVideoClick(video.videoID)}
              >
                {video.title || "暂无标题"}
              </div>
              <div
                className="video-recommended-author"
                onClick={(e) => handleAuthorClick(video.uploaderID, e)}
              >
                UP主: {video.uploaderInfo?.username || "未知用户"}
              </div>
              <div className="video-recommended-meta">
                <span>点赞: {video.likes || 0}</span>
                <span>收藏: {video.favorites || 0}</span>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SidebarSection;