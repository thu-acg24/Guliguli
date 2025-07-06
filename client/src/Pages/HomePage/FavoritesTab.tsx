// src/Pages/HomePage/FavoritesTab.tsx
import React, { useState, useEffect } from "react";
import { Video } from "Plugins/VideoService/Objects/Video";
import { useOutletContext, useNavigate } from "react-router-dom";
import { QueryFavoriteVideosMessage } from "Plugins/VideoService/APIs/QueryFavoriteVideosMessage";
import { videoPagePath } from "Pages/VideoPage/VideoPage";
import "./HomePage.css";

const FavoritesTab: React.FC<{ userID?: number }> = (props) => {
    const outlet = useOutletContext<{ userID: number, isCurrentUser: boolean }>();
    const userID = props.userID ?? outlet?.userID;
    const navigate = useNavigate();

    const [videos, setVideos] = useState<Video[]>([]);
    const [loading, setLoading] = useState(true);

    // 获取用户收藏的视频
    const fetchFavorites = async () => {
        setLoading(true);
        try {
            new QueryFavoriteVideosMessage(userID).send(
                (info: string) => {
                    const videos = JSON.parse(info) as Video[];
                    setVideos(videos);
                }, (error: any) => {
                    throw new Error(error);
                }
            );
        } catch (error) {
            console.error("获取收藏失败", error);
        } finally {
            setLoading(false);
        }
    };

    // 处理视频点击
    const handleVideoClick = (videoID: number) => {
        const videoPath = videoPagePath.replace(":video_id", videoID.toString());
        navigate(videoPath);
    };

    useEffect(() => {
        if (userID) {
            fetchFavorites();
        }
    }, [userID]);

    return (
        <div className="home-favorites-tab">
            {loading ? (
                <div className="home-loading">
                    <div className="home-loading-text">加载中...</div>
                </div>
            ) : (
                <div className="home-video-list">
                    {videos.map(video => (
                        <div key={video.videoID} className="home-video-item" onClick={() => handleVideoClick(video.videoID)}>
                            <div className="home-video-cover-container">
                                <img src={video.coverPath} alt="视频封面" className="home-video-cover" />
                            </div>
                            <div className="home-video-info">
                                <div className="home-video-title">{video.title}</div>
                                <div className="home-video-meta">
                                    <span>{video.views} 播放</span>
                                    <span>{video.likes} 点赞</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default FavoritesTab;