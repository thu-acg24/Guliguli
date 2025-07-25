import React, { useState, useEffect } from "react";
import { useNavigateVideo } from "Globals/Navigate";
import { useUserToken } from "Globals/GlobalStore";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { QueryPendingVideosMessage } from "Plugins/VideoService/APIs/QueryPendingVideosMessage";
import { ChangeVideoStatusMessage } from "Plugins/VideoService/APIs/ChangeVideoStatusMessage";
import { Video } from "Plugins/VideoService/Objects/Video";
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus";
import DefaultCover from "Images/DefaultCover.jpg";
import { useTopSuccessToast } from "Components/TopSuccessToast/useTopSuccessToast";
import { formatTime, formatDuration } from "Components/Formatter";

const VideoAudit: React.FC = () => {
    const { navigateVideo } = useNavigateVideo();
    const userToken = useUserToken();
    const [videos, setVideos] = useState<Video[]>([]);
    const [loading, setLoading] = useState(true);
    const { ToastComponent, showSuccess } = useTopSuccessToast();

    useEffect(() => {
        loadPendingVideos();
    }, []);

    const loadPendingVideos = async () => {
        if (!userToken) return;

        try {
            setLoading(true);
            const videosData = await new Promise<Video[]>((resolve, reject) => {
                new QueryPendingVideosMessage(userToken).send(
                    (info: string) => resolve(JSON.parse(info) as Video[]),
                    (error: string) => reject(new Error(error))
                );
            });
            setVideos(videosData);
        } catch (error) {
            console.error("获取待审核视频失败", error);
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取待审核视频失败");
        } finally {
            setLoading(false);
        }
    };

    const handleVideoAction = async (videoID: number, status: VideoStatus) => {
        try {
            await new Promise<void>((resolve, reject) => {
                new ChangeVideoStatusMessage(userToken, videoID, status).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });

            // 从列表中移除已处理的视频
            setVideos(prev => prev.filter(video => video.videoID !== videoID));
            showSuccess(`操作成功`);
        } catch (error) {
            console.error("视频审核操作失败", error);
            materialAlertError("操作失败", error instanceof Error ? error.message : ``);
        }
    };

    const handleViewVideo = (videoID: number) => {
        navigateVideo(videoID);
    };

    if (loading) {
        return (
            <div className="video-audit">
                <div className="video-audit-header">
                    <h1 className="video-audit-title">视频审核</h1>
                    <p className="video-audit-subtitle">加载中...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="video-audit">
            {ToastComponent}
            <div className="video-audit-header">
                <h1 className="video-audit-title">视频审核</h1>
                <p className="video-audit-subtitle">
                    共有 {videos.length} 个待审核视频
                </p>
            </div>

            {videos.length === 0 ? (
                <div className="audit-empty">
                    <div className="audit-empty-icon">📹</div>
                    <div className="audit-empty-text">暂无待审核的视频</div>
                </div>
            ) : (
                <div className="video-audit-list">
                    {videos.map(video => (
                        <div key={video.videoID} className="video-audit-item">
                            <div className="audit-video-cover-container"
                                onClick={() => handleViewVideo(video.videoID)}>
                                <img
                                    src={video.cover || DefaultCover}
                                    alt="视频封面"
                                    className="audit-video-cover"
                                    onError={(e) => {
                                        (e.target as HTMLImageElement).src = DefaultCover;
                                    }}
                                />
                                {video.duration &&
                                    <div className="audit-video-duration">
                                        {formatDuration(video.duration)}
                                    </div>}
                            </div>
                            <div className="audit-video-info">
                                <h3 className="audit-video-title"
                                    onClick={() => handleViewVideo(video.videoID)}>
                                    {video.title}
                                </h3>
                                <div className="audit-video-description">
                                    {video.description || " "}
                                </div>
                                <div className="audit-video-meta">
                                    上传时间: {formatTime(video.uploadTime, false)}
                                </div>
                                {video.tag && video.tag.length > 0 && (
                                    <div className="audit-video-tags">
                                        {video.tag.map((tag: string, index: number) => (
                                            <span key={index} className="audit-video-tag">
                                                {tag}
                                            </span>
                                        ))}
                                    </div>
                                )}
                            </div>
                            <div className="danmaku-report-actions">
                                <button
                                    className="danmaku-action-btn danmaku-action-approve"
                                    onClick={() => handleVideoAction(video.videoID, VideoStatus.approved)}
                                >
                                    ✓
                                </button>
                                <button
                                    className="danmaku-action-btn danmaku-action-reject"
                                    onClick={() => handleVideoAction(video.videoID, VideoStatus.rejected)}
                                >
                                    ×
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default VideoAudit;
