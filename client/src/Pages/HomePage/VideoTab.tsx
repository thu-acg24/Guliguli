// src/Pages/HomePage/VideoTab.tsx
import React, { useState, useEffect } from "react";
import { Video } from "Plugins/VideoService/Objects/Video";
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus";
import { useOutletContext } from "react-router-dom";
import "./HomePage.css";

interface VideoTabProps {
    userID: number;
}

const VideoTab: React.FC<{ userID?: number }> = (props) => {
    const outlet = useOutletContext<{ userID: number }>();
    const userID = props.userID ?? outlet?.userID;

    const [videos, setVideos] = useState<Video[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(true);

    // 获取用户发布的视频
    const fetchVideos = async (page: number) => {
        setLoading(true);
        try {
            // API调用留空
            // const result = await getUserVideos(userID, page, 10);
            // setVideos(prev => [...prev, ...result.videos]);
            // setHasMore(result.hasMore);

            // 模拟数据
            const mockVideos = Array.from({ length: 10 }, (_, i) =>
                new Video(
                    i + (page - 1) * 10,
                    `视频标题 ${i + (page - 1) * 10}`,
                    "视频描述",
                    120,
                    ["标签1", "标签2"],
                    "",
                    "https://picsum.photos/300/169",
                    userID,
                    Math.floor(Math.random() * 10000),
                    Math.floor(Math.random() * 1000),
                    Math.floor(Math.random() * 500),
                    VideoStatus.approved,
                    Date.now()
                )
            );

            setVideos(prev => [...prev, ...mockVideos]);
            setHasMore(true);
        } catch (error) {
            console.error("获取视频失败", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchVideos(page);
    }, [page]);

    const handleLoadMore = () => {
        setPage(prev => prev + 1);
    };

    return (
        <div className="home-video-tab">
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

            {hasMore && (
                <div className="home-load-more" onClick={handleLoadMore}>
                    {loading ? "加载中..." : "加载更多"}
                </div>
            )}
        </div>
    );
};

export default VideoTab;