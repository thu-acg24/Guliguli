// src/Pages/HomePage/HistoryTab.tsx
import React, { useState, useEffect } from "react";
import { useOutletContext } from "react-router-dom";
import { useUserToken } from "Globals/GlobalStore";
import expiredVideoCover from "Images/ExpiredVideo.jpg";
import { Video } from "Plugins/VideoService/Objects/Video";
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus";
import { QueryVideoInfoMessage } from "Plugins/VideoService/APIs/QueryVideoInfoMessage";
import { QueryHistoryMessage } from "Plugins/HistoryService/APIs/QueryHistoryMessage";
import "./HomePage.css";

const perpage = 10; // 每次新显示的历史记录数量

const HistoryTab: React.FC<{ userID?: number }> = (props) => {
    const outlet = useOutletContext<{ userID: number, isCurrentUser: boolean }>();
    const userToken = useUserToken();

    const [videos, setVideos] = useState<Video[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(true);

    // 获取用户观看历史
    const fetchHistory = async (page: number) => {
        setLoading(true);
        try {
            const newVideosID = await new Promise<number[]>((resolve, reject) => {
                new QueryHistoryMessage(userToken, (page - 1) * perpage + 1, page * perpage).send(
                    (info: string) => {
                        const historyList = JSON.parse(info);
                        resolve(historyList.map((record: any) => record.videoID));
                    },
                    (error: any) => {
                        console.error("获取历史记录失败", error);
                        reject(error);
                    }
                );
            });

            const newVideos = await Promise.all(
                newVideosID.map(async (id) => {
                    return await new Promise<Video>((resolve, reject) => {
                        new QueryVideoInfoMessage(userToken, id).send(
                            (info: string) => {
                                resolve(JSON.parse(info) as Video);
                            },
                            (error: any) => {
                                console.error("获取视频信息失败", error);
                                resolve(new Video(
                                    0,
                                    "视频已失效",
                                    "该视频已被删除或不存在",
                                    0,
                                    [],
                                    "",
                                    expiredVideoCover,
                                    0,
                                    0,
                                    0,
                                    0,
                                    VideoStatus.rejected,
                                    null
                                ))
                            }
                        );
                    });
                })
            );

            const hasMore = newVideos.length === perpage;

            // 模拟数据
            // const newVideos = Array.from({ length: 10 }, (_, i) =>
            //     new Video(
            //         i + (page - 1) * 10,
            //         `观看过的视频 ${i + (page - 1) * 10}`,
            //         "视频描述",
            //         120,
            //         ["标签1", "标签2"],
            //         "",
            //         "https://picsum.photos/300/169",
            //         userID,
            //         Math.floor(Math.random() * 10000),
            //         Math.floor(Math.random() * 1000),
            //         Math.floor(Math.random() * 500),
            //         VideoStatus.approved,
            //         Date.now()
            //     )
            // );
            // const hasMore = true

            setVideos(prev => [...prev, ...newVideos]);
            setHasMore(hasMore);
        } catch (error) {
            console.error("获取历史记录失败", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchHistory(page);
    }, [page]);

    const handleLoadMore = () => {
        setPage(prev => prev + 1);
    };

    return (
        <div className="home-history-tab">
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

export default HistoryTab;