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
import { DeleteHistoryMessage } from "Plugins/HistoryService/APIs/DeleteHistoryMessage";
import { ClearHistoryMessage } from "Plugins/HistoryService/APIs/ClearHistoryMessage";
import { useTopSuccessToast } from "Components/TopSuccessToast/useTopSuccessToast";

const perpage = 10; // 每次新显示的历史记录数量

interface HistoryWithVideo {
    history: HistoryRecord;
    video: Video;
}

const HistoryTab: React.FC<{ userID?: number }> = (props) => {
    const outlet = useOutletContext<{ userID: number, isCurrentUser: boolean }>();
    const userToken = useUserToken();
    const { navigateVideo } = useNavigateVideo();
    const { ToastComponent, showSuccess } = useTopSuccessToast();

    const [videos, setVideos] = useState<HistoryWithVideo[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(true);
    const [refreshTag, setRefreshTag] = useState(false);

    // 获取用户观看历史
    const fetchHistory = async (page: number) => {
        setLoading(true);
        try {
            const historyList = await new Promise<HistoryRecord[]>((resolve, reject) => {
                new QueryHistoryMessage(
                    userToken,
                    page === 1
                        ? 99999999999999
                        : videos[videos.length - 1].history.timestamp,
                    page === 1
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

            if (page === 1) {
                setVideos(newHistoryWithVideos);
            } else {
                setVideos(prev => [...prev, ...newHistoryWithVideos]);
            }
            setHasMore(hasMore);
        } catch (error) {
            console.error("获取历史记录失败", error);
            materialAlertError("获取历史记录失败，请稍后重试。");
        } finally {
            setLoading(false);
        }
    };

    const refresh = () => {
        setRefreshTag(prev => !prev);
        setPage(1);
    };

    useEffect(() => {
        fetchHistory(page);
    }, [page, refreshTag]);

    const handleLoadMore = () => {
        setPage(prev => prev + 1);
    };

    // 处理视频点击
    const handleVideoClick = (videoID: number) => {
        navigateVideo(videoID);
    };

    const handleDeleteClick = async (videoID: number) => {
        try {
            await new Promise<void>((resolve, reject) => {
                new DeleteHistoryMessage(userToken, videoID).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });
            showSuccess("历史记录已删除");
            refresh();
        } catch (error) {
            console.error("删除历史记录失败", error);
            materialAlertError("删除历史记录失败，请稍后重试。");
        }
    }

    const handleClearClick = async () => {
        try {
            await new Promise<void>((resolve, reject) => {
                new ClearHistoryMessage(userToken).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });
            showSuccess("观看历史已清空");
            refresh();
        } catch (error) {
            console.error("清空观看历史失败", error);
            materialAlertError("清空观看历史失败，请稍后重试。");
        }
    }

    return (
        <>
            {ToastComponent}
            <div className="home-tab-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span>观看历史</span>
                <button
                    className="home-follow-btn"
                    onClick={() => handleClearClick()}
                >
                    清空
                </button>
            </div>
            <div className="home-history-tab">
                <div className="home-video-list">
                    {videos.map(({ history, video }) => (
                        <div key={history.historyID} className="home-video-item">
                            <div className="home-video-cover-container">
                                <img src={video.cover || DefaultCover} alt="视频封面" className="home-video-cover" onClick={() => handleVideoClick(video.videoID)} />
                            </div>
                            <div className="home-video-info">
                                <div className="home-video-title" onClick={() => handleVideoClick(video.videoID)}>{video.title}</div>
                                <div className="home-video-meta" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                                    <span>观看时间：{dateformatTime(history.timestamp)}</span>
                                    <span
                                        className="home-history-remove"
                                        onClick={() => handleDeleteClick(history.videoID)}
                                    >
                                        ×
                                    </span>
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
        </>
    );
};

export default HistoryTab;