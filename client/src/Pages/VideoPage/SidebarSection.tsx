import React, { useState } from "react";
import "./SidebarSection.css";
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { Video } from 'Plugins/VideoService/Objects/Video';
import { UserStat } from 'Plugins/UserService/Objects/UserStat';
import { useNavigate } from "react-router-dom";
import { useNavigateHome, useNavigateVideo } from "Globals/Navigate";
import DefaultCover from "Images/DefaultCover.jpg";
import { SimpleVideo } from "Components/RecommendVideoService";
import { formatCount } from "Components/Formatter";
import { Danmaku as DanmakuObj } from "Plugins/DanmakuService/Objects/Danmaku";
import { QueryVideoDanmakuMessage } from "Plugins/DanmakuService/APIs/QueryVideoDanmakuMessage";
import { formatDuration } from "Components/Formatter";
import {UpUserIcon,HollowLikeIcon,PlayCountIcon} from "Images/Icons"

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
  const { navigateHome } = useNavigateHome();
  const { navigateVideo } = useNavigateVideo();
  const [showDanmakuList, setShowDanmakuList] = useState(false);
  const [danmakuList, setDanmakuList] = useState<DanmakuObj[]>([]);
  const [loadingDanmaku, setLoadingDanmaku] = useState(false);

  const handleVideoClick = (videoId: number) => {
    if (videoId > 0) {
      navigateVideo(videoId);
    }
  };

  const handleAuthorClick = (userId: number, e: React.MouseEvent) => {
    e.stopPropagation();
    if (userId > 0) {
      navigateHome(userId);
    }
  };

  const toggleDanmakuList = async () => {
    if (!showDanmakuList && danmakuList.length === 0) {
      setLoadingDanmaku(true);
      try {
        const danmakus = await new Promise<DanmakuObj[]>((resolve, reject) => {
          new QueryVideoDanmakuMessage(videoInfo.videoID).send(
            (res: string) => resolve(JSON.parse(res)),
            (error: string) => reject(new Error(error))
        )});
        setDanmakuList(danmakus);
      } catch (error) {
        console.error("加载弹幕失败:", error);
      } finally {
        setLoadingDanmaku(false);
      }
    }
    setShowDanmakuList(!showDanmakuList);
  };

  const handleDanmakuClick = (timeInVideo: number) => {
    const videoPlayer = document.querySelector('video');
    if (videoPlayer) {
      videoPlayer.currentTime = timeInVideo;
      videoPlayer.play();
    }
  };

  const handleReportDanmaku = (danmaku: DanmakuObj) => {
    alert(`举报弹幕: ${danmaku.content}\n时间: ${formatDuration(danmaku.timeInVideo)}`);
  };

  return (
    <div className="video-video-sidebar">
      <div className="video-up-info">
        <div className="video-up-top-row">
          <div className="video-up-avatar">
            <img 
              src={uploaderInfo.avatarPath} 
              alt="UP主头像" 
              onClick={() => navigateHome(uploaderInfo.userID)}
              onError={(e) => { e.currentTarget.src = DefaultCover; }}
            />
          </div>
          <div className="video-up-details">
            <div className="video-up-name">
              <span onClick={() => navigateHome(uploaderInfo.userID)}>
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
            {isFollowing ? '已关注' : '关注'}&nbsp;{formatCount(upstat.followerCount)}
          </button>
        )}
      </div>

      <div className="video-recommended-videos">
        <div className="video-recommended-big-title">
          弹幕列表
          <button 
            onClick={toggleDanmakuList}
            style={{
              float: 'right',
              background: 'none',
              border: 'none',
              color: '#00a1d6',
              cursor: 'pointer',
              fontSize: '12px'
            }}
          >
            {showDanmakuList ? '收起弹幕' : '查看弹幕'}
          </button>
        </div>
        {showDanmakuList && (
          <div className="video-danmaku-list">
            {loadingDanmaku ? (
              <div style={{ padding: '10px', textAlign: 'center' }}>加载弹幕中...</div>
            ) : (
              <ul style={{ 
                maxHeight: '300px', 
                overflowY: 'auto',
                marginBottom: '15px',
                border: '1px solid #f0f0f0',
                borderRadius: '4px'
              }}>
                {danmakuList.length === 0 ? (
                  <div style={{ padding: '10px', textAlign: 'center', color: '#999' }}>暂无弹幕</div>
                ) : (
                  danmakuList.map((danmaku, index) => (
                    <li 
                      key={index}
                      style={{
                        padding: '8px 12px',
                        borderBottom: '1px solid #f5f5f5',
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        cursor: 'pointer',
                        fontSize: '14px'

                      }}
                      onClick={() => handleDanmakuClick(danmaku.timeInVideo)}
                      onMouseEnter={(e) => {
                        const target = e.currentTarget;
                        const reportBtn = target.querySelector('.danmaku-report-btn');
                        if (reportBtn) {
                          (reportBtn as HTMLElement).style.display = 'inline-block';
                        }
                      }}
                      onMouseLeave={(e) => {
                        const target = e.currentTarget;
                        const reportBtn = target.querySelector('.danmaku-report-btn');
                        if (reportBtn) {
                          (reportBtn as HTMLElement).style.display = 'none';
                        }
                      }}
                    >
                      <div>
                        <span style={{ color: '#999', marginRight: '8px' }}>
                          {formatDuration(danmaku.timeInVideo)}
                        </span>
                        <span>{danmaku.content}</span>
                      </div>
                      <button
                        className="danmaku-report-btn"
                        style={{
                          display: 'none',
                          background: 'none',
                          border: 'none',
                          color: '#ff4d4f',
                          cursor: 'pointer',
                          fontSize: '14px'
                        }}
                        onClick={(e) => {
                          e.stopPropagation();
                          handleReportDanmaku(danmaku);
                        }}
                      >
                        举报
                      </button>
                    </li>
                  ))
                )}
              </ul>
            )}
          </div>
        )}
      </div>

      <div className="video-recommended-videos">
        <div className="video-recommended-big-title">
          推荐视频
        </div>
        

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
              <UpUserIcon className="video-recommended-icon" />
              {video.uploaderInfo?.username || "未知用户"}
              </div>
              <div className="video-recommended-meta">
                <div className="video-icon-meta">
                  <HollowLikeIcon className="video-recommended-like-icon" />
                  {formatCount(video.likes) || 0}
                </div>
                <div className="video-icon-meta">
                  <PlayCountIcon className="video-recommended-icon" />
                  {formatCount(video.views) || 0}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SidebarSection;