// src/Pages/HomePage/VideoTab.tsx
import React, { useState, useEffect } from "react";
import { Video } from "Plugins/VideoService/Objects/Video";
import { useOutletContext } from "react-router-dom";
import { QueryUserVideosMessage } from "Plugins/VideoService/APIs/QueryUserVideosMessage";
import "./HomePage.css";

const VideoTab: React.FC<{ userID?: number }> = (props) => {
    const outlet = useOutletContext<{ userID: number, isCurrentUser: boolean }>();
    const userID = props.userID ?? outlet?.userID;

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

    useEffect(() => {
        if (userID) {
            fetchVideos();
        }
    }, [userID]);

    return (
        <div className="home-video-tab">
            {loading ? (
                <div className="home-loading">
                    <div className="home-loading-text">加载中...</div>
                </div>
            ) : (
                <div className="home-video-list">
                    {videos.map(video => (
                        <div key={video.videoID} className="home-video-item">
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

export default VideoTab;