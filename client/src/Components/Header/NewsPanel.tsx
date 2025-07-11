import React from 'react';
import { VideoWithUploader } from './Header';
import { formatTime } from "../Formatter";
import { useNavigateHome, useNavigateVideo } from "Globals/Navigate";
import DefaultAvatar from "Images/DefaultAvatar.jpg";
import DefaultCover from "Images/DefaultCover.jpg";

const NewsPanel: React.FC<{ show: boolean; videos: VideoWithUploader[] }> = ({ show, videos }) => {
    if (!show) return null;
    const { navigateHome } = useNavigateHome();
    const { navigateVideo } = useNavigateVideo();
    const handleUserClick = (userID: number) => {
        navigateHome(userID);
    }
    const handleVideoClick = (videoID: number) => {
        navigateVideo(videoID);
    };
    return (
        <div className="header-news-wrapper">
            <div className="header-news-panel">
                {videos.length === 0 ? (
                    <div className="header-news-empty">暂无动态</div>
                ) : (
                    <div className="header-news-list">
                        {videos.map(({ video, uploaderInfo }) => (
                            <div
                                className="header-news-list-item"
                            >
                                <div className="header-news-container">
                                    <div className="header-news__box--left">
                                        <div className="guli-avatar" style={{ width: '100%', height: '100%' }} onClick={() => handleUserClick(uploaderInfo.userID)}>
                                            <img
                                                className="guli-avatar-img"
                                                src={uploaderInfo.avatarPath || DefaultAvatar}
                                            />
                                        </div>
                                    </div>
                                    <div className="header-news__box--center">
                                        <div className="news-name-line">
                                            <div className="header-user-name" onClick={() => handleUserClick(uploaderInfo.userID)}>
                                                {uploaderInfo.username}
                                            </div>
                                        </div>
                                        <div className="news-info-container" onClick={() => handleVideoClick(video.videoID)}>
                                            <div className="news-info__box--left">
                                                <div className="news-info-content">
                                                    <div className="all-in-one-article-title">{video.title}</div>
                                                </div>
                                                <span className="publish-time">{formatTime(video.uploadTime, true)}</span>
                                            </div>
                                            <div className="news-info__box--right">
                                                <div className="cover">
                                                    <img
                                                        src={video.cover || DefaultCover}
                                                        loading="lazy"
                                                    />
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

export default NewsPanel;