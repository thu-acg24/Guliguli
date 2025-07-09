// src/Pages/HomePage/HistoryTab.tsx
import React, { useState, useEffect } from "react";
import { useOutletContext } from "react-router-dom";
import { useNavigateVideo } from "Globals/Navigate";
import { useUserToken } from "Globals/GlobalStore";
import DefaultCover from "Images/DefaultCover.jpg";
import { Video } from "Plugins/VideoService/Objects/Video";
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus";
import { QueryVideoInfoMessage } from "Plugins/VideoService/APIs/QueryVideoInfoMessage";
import { QueryHistoryMessage } from "Plugins/HistoryService/APIs/QueryHistoryMessage";
import "./HomePage.css";
import { HistoryRecord } from "Plugins/HistoryService/Objects/HistoryRecord";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { dateformatTime } from "Components/Formatter";

const perpage = 10; // 每次新显示的历史记录数量

interface HistoryWithVideo {
    history: HistoryRecord;
    video: Video;
}

const HistoryTab: React.FC<{ userID?: number }> = (props) => {
    const outlet = useOutletContext<{ userID: number, isCurrentUser: boolean }>();
    const userToken = useUserToken();
    const { navigateVideo } = useNavigateVideo();

    const [videos, setVideos] = useState<HistoryWithVideo[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(true);

    // 获取用户观看历史
    const fetchHistory = async (page: number) => {
        setLoading(true);
        try {
            const historyList = await new Promise<HistoryRecord[]>((resolve, reject) => {
                new QueryHistoryMessage(
                    userToken,
                    videos.length === 0
                        ? 99999999999999
                        : videos[videos.length - 1].history.timestamp,
                    videos.length === 0
                        ? 9999
                        : videos[videos.length - 1].history.videoID,
                    perpage
                ).send(
                    (info: string) => resolve(JSON.parse(info) as HistoryRecord[]),
                    (error: any) => {
                        console.error("获取历史记录失败", error);
                        reject(error);
                    }
                );
            });

            const newHistoryWithVideos = await Promise.all(
                historyList.map(async (record: HistoryRecord) => {
                    const video = await new Promise<Video>((resolve, reject) => {
                        new QueryVideoInfoMessage(userToken, record.videoID).send(
                            (info: string) => {
                                resolve(JSON.parse(info) as Video);
                            },
                            (error: any) => {
                                console.error("获取视频信息失败", error);
                                resolve(new Video(
                                    0,
                                    "视频已失效",
                                    "该视频已被删除或不存在",
                                    null,
                                    null,
                                    [],
                                    0,
                                    0,
                                    0,
                                    0,
                                    VideoStatus.broken,
                                    null
                                ))
                            }
                        );
                    });
                    return { history: record, video } as HistoryWithVideo;
                })
            );

            const hasMore = newHistoryWithVideos.length === perpage;

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

            setVideos(prev => [...prev, ...newHistoryWithVideos]);
            setHasMore(hasMore);
        } catch (error) {
            console.error("获取历史记录失败", error);
            materialAlertError("获取历史记录失败，请稍后重试。");
        } finally {
            setLoading(false);
        }
    };

    // 处理视频点击
    const handleVideoClick = (videoID: number) => {
        navigateVideo(videoID);
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
                {videos.map(({ history, video }) => (
                    <div key={history.historyID} className="home-video-item" onClick={() => handleVideoClick(video.videoID)}>
                        <div className="home-video-cover-container">
                            <img src={video.cover || DefaultCover} alt="视频封面" className="home-video-cover" />
                        </div>
                        <div className="home-video-info">
                            <div className="home-video-title">{video.title}</div>
                            <div className="home-video-meta">
                                <span>观看时间：{dateformatTime(video.views)}</span>
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