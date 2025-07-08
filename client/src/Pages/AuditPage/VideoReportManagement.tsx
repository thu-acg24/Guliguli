import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useUserToken } from "Globals/GlobalStore";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { QueryVideoReportsMessage } from "Plugins/ReportService/APIs/QueryVideoReportsMessage";
import { ProcessVideoReportMessage } from "Plugins/ReportService/APIs/ProcessVideoReportMessage";
import { QueryVideoInfoMessage } from "Plugins/VideoService/APIs/QueryVideoInfoMessage";
import { ReportVideo } from "Plugins/ReportService/Objects/ReportVideo";
import { ReportStatus } from "Plugins/ReportService/Objects/ReportStatus";
import { Video } from "Plugins/VideoService/Objects/Video";
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus";
import { videoPagePath } from "Pages/VideoPage/VideoPage";
import DefaultCover from "Images/DefaultCover.jpg";
import { useTopSuccessToast } from "Components/TopSuccessToast/useTopSuccessToast";

const VideoReportManagement: React.FC = () => {
    interface ReportWithVideo {
        report: ReportVideo;
        video: Video;
    }

    const navigate = useNavigate();
    const userToken = useUserToken();
    const [reports, setReports] = useState<ReportWithVideo[]>([]);
    const [loading, setLoading] = useState(true);
    const { ToastComponent, showSuccess } = useTopSuccessToast();

    useEffect(() => {
        loadReports();
    }, []);

    const loadReports = async () => {
        if (!userToken) return;

        try {
            setLoading(true);
            const response = await new Promise<string>((resolve, reject) => {
                new QueryVideoReportsMessage(userToken).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const reportsData = JSON.parse(response) as ReportVideo[];

            // 获取每个举报对应的视频信息
            const reportsWithVideo = await Promise.all(
                reportsData.map(async (report) => {
                    try {
                        const videoResponse = await new Promise<string>((resolve, reject) => {
                            new QueryVideoInfoMessage(userToken, report.videoID).send(
                                (info: string) => resolve(info),
                                (error: string) => reject(new Error(error))
                            );
                        });
                        const video = JSON.parse(videoResponse) as Video;
                        return { report, video };
                    } catch (error) {
                        console.warn(`获取视频 ${report.videoID} 信息失败:`, error);
                        // 如果获取视频信息失败，创建一个默认的 Video 对象
                        const defaultVideo = new Video(
                            report.videoID,
                            "视频信息获取失败",
                            "",
                            null,
                            null,
                            [],
                            0,
                            0,
                            0,
                            0,
                            VideoStatus.broken,
                            0
                        );
                        return { report, video: defaultVideo };
                    }
                })
            );

            setReports(reportsWithVideo);
        } catch (error) {
            console.error("获取视频举报失败", error);
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取视频举报失败");
        } finally {
            setLoading(false);
        }
    };

    const handleReportAction = async (reportID: number, status: ReportStatus) => {
        try {
            await new Promise<void>((resolve, reject) => {
                new ProcessVideoReportMessage(userToken, reportID, status).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });

            // 从列表中移除已处理的举报
            setReports(prev => prev.filter(item => item.report.reportID !== reportID));
            showSuccess(`处理成功`);
        } catch (error) {
            console.error("处理举报失败", error);
            materialAlertError("操作失败", error instanceof Error ? error.message : ``);
        }
    };

    const handleViewVideo = (videoID: number) => {
        const videoPath = videoPagePath.replace(":video_id", videoID.toString());
        navigate(videoPath);
    };

    const formatDate = (timestamp: number): string => {
        return new Date(timestamp).toLocaleString('zh-CN');
    };

    const formatDuration = (seconds: number): string => {
        const mins = Math.floor(seconds / 60);
        const secs = Math.floor(seconds % 60);
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    if (loading) {
        return (
            <div className="report-management">
                <div className="report-header">
                    <h1 className="report-title">视频举报管理</h1>
                    <p className="report-subtitle">加载中...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="report-management">
            {ToastComponent}
            <div className="report-header">
                <h1 className="report-title">视频举报管理</h1>
                <p className="report-subtitle">
                    共有 {reports.length} 个待处理举报
                </p>
            </div>

            {reports.length === 0 ? (
                <div className="audit-empty">
                    <div className="audit-empty-icon">📋</div>
                    <div className="audit-empty-text">暂无待处理的视频举报</div>
                </div>
            ) : (
                <div className="video-audit-list">
                    {reports.map(item => (
                        <div key={item.report.reportID} className="video-audit-item">
                            <div className="audit-video-cover-container"
                                onClick={() => handleViewVideo(item.video.videoID)}
                                style={{ width: '160px', height: '90px', flexShrink: 0 }}>
                                <img
                                    src={item.video.cover || DefaultCover}
                                    alt="视频封面"
                                    className="audit-video-cover"
                                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                    onError={(e) => {
                                        (e.target as HTMLImageElement).src = DefaultCover;
                                    }}
                                />
                                {item.video.duration &&
                                    <div className="audit-video-duration">
                                        {formatDuration(item.video.duration)}
                                    </div>}
                            </div>
                            <div className="audit-video-info">
                                <h3 className="audit-video-title"
                                    onClick={() => handleViewVideo(item.video.videoID)}>
                                    {item.video.title}
                                </h3>
                                <div className="audit-video-description">
                                    {item.video.description || " "}
                                </div>
                                <div className="audit-video-meta">
                                    上传时间: {formatDate(item.video.uploadTime)}
                                </div>
                                {item.video.tag && item.video.tag.length > 0 && (
                                    <div className="audit-video-tags">
                                        {item.video.tag.map((tag: string, index: number) => (
                                            <span key={index} className="audit-video-tag">
                                                {tag}
                                            </span>
                                        ))}
                                    </div>
                                )}
                            </div>
                            <div className="audit-report-details">
                                <div className="danmaku-report-reason">
                                    <strong>举报原因：</strong>{item.report.reason}
                                </div>
                                <div className="danmaku-report-meta">
                                    举报时间: {formatDate(item.report.timestamp)}
                                </div>
                            </div>
                            <div className="danmaku-report-actions">
                                <button
                                    className="danmaku-action-btn danmaku-action-view"
                                    onClick={() => handleViewVideo(item.video.videoID)}
                                >
                                    查看视频
                                </button>
                                <button
                                    className="danmaku-action-btn danmaku-action-approve"
                                    onClick={() => handleReportAction(item.report.reportID, ReportStatus.resolved)}
                                >
                                    ✓
                                </button>
                                <button
                                    className="danmaku-action-btn danmaku-action-reject"
                                    onClick={() => handleReportAction(item.report.reportID, ReportStatus.rejected)}
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

export default VideoReportManagement;
