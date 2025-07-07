import React from "react";
import "./VideoPage.css";
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { Video } from 'Plugins/VideoService/Objects/Video';
import {UserStat} from 'Plugins/UserService/Objects/UserStat'

interface SidebarSectionProps {
  uploaderInfo: UserInfo | null;
  videoinfo: Video;
  userToken: string;
  userInfo: UserInfo;
  isFollowing: boolean;
  upstat: UserStat;
  followUp: (id: number) => void;
  navigateToUser: (id: number) => void;
  recommendedVideos: any[];
}

const SidebarSection: React.FC<SidebarSectionProps> = ({
  uploaderInfo,
  videoinfo,
  userToken,
  userInfo,
  isFollowing,
  upstat,
  followUp,
  navigateToUser,
  recommendedVideos,
}) => {
  return (
    <div className="video-video-sidebar">
      <div className="video-up-info">
        <div className="video-up-top-row">
          <div className="video-up-avatar">
            <img 
              src={uploaderInfo.avatarPath} 
              alt="UP主头像" 
              onClick={() => navigateToUser(uploaderInfo.userID)}
            />
          </div>
          <div className="video-up-details">
            <div className="video-up-name">
              <span
                onClick={() => navigateToUser(uploaderInfo.userID)}
              >
                {uploaderInfo.username}
              </span>
            </div>
            <div className="video-up-description" title={uploaderInfo.bio}>
              {uploaderInfo.bio.length > 17
                ? `${uploaderInfo.bio.substring(0, 17)}...`
                : uploaderInfo.bio}
            </div>
          </div>
        </div>
        {((!userToken) || (videoinfo.uploaderID !== userInfo?.userID)) && (
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
    </div>
  );
};

export default SidebarSection;