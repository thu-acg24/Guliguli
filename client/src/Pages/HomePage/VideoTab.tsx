// src/Pages/HomePage/VideoTab.tsx
import React, { useState, useEffect } from "react";
import { useOutletContext } from "react-router-dom";
import { useNavigateVideo } from "Globals/Navigate";
import DefaultCover from "Images/DefaultCover.jpg";
import { videoPagePath } from "Pages/VideoPage/VideoPage";
import { Video } from "Plugins/VideoService/Objects/Video";
import { QueryUserVideosMessage } from "Plugins/VideoService/APIs/QueryUserVideosMessage";
import "./HomePage.css";

const VideoTab: React.FC<{ userID?: number }> = (props) => {
    const outlet = useOutletContext<{ userID: number, isCurrentUser: boolean }>();
    const userID = props.userID ?? outlet?.userID;
    const { navigateVideo } = useNavigateVideo();

    const [videos, setVideos] = useState<Video[]>([]);
    const [loading, setLoading] = useState(true);

    // 获取用户发布的视频
    const fetchVideos = async () => {
        setLoading(true);
        try {
            new QueryUserVideosMessage(null, userID).send(
                (info: string) => {
                    const videos = JSON.parse(info) as Video[];
                    setVideos(videos);
                }, (error: any) => {
                    throw new Error(error);
                }
            );
        } catch (error) {
            console.error("获取用户视频失败", error);
        } finally {
            setLoading(false);
        }
    };

    // 处理视频点击
    const handleVideoClick = (videoID: number) => {
        navigateVideo(videoID);
    };

    useEffect(() => {
        if (userID) {
            fetchVideos();
        }
    }, [userID]);

    return (
        <>
            <div className="home-tab-title">发布的视频</div>
            <div className="home-video-tab">
                {loading ? (
                    <div className="home-loading">
                        <div className="home-loading-text">加载中...</div>
                    </div>
                ) : (
                    <div className="home-video-list">
                        {videos.map(video => (
                            <div key={video.videoID} className="home-video-item" onClick={() => handleVideoClick(video.videoID)}>
                                <div className="home-video-cover-container">
                                    <img src={video.cover || DefaultCover} alt="视频封面" className="home-video-cover" />
                                </div>
                                <div className="home-video-info">
                                    <div className="home-video-title">{video.title}</div>
                                    <div className="home-video-meta">
                                        <span className="home-video-meta-item">{video.views} 播放</span>
                                        <span className="home-video-meta-item">{video.likes} 点赞</span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </>
    );
};

export default VideoTab;