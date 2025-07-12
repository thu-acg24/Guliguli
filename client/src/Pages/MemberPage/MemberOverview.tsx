import React, { useState, useEffect } from "react";
import { MemberPageTab, useNavigateMember, useNavigateVideo } from "Globals/Navigate";
import { useUserToken, useUserID } from "Globals/GlobalStore";
import { QueryUserVideosMessage } from "Plugins/VideoService/APIs/QueryUserVideosMessage";
import { Video } from "Plugins/VideoService/Objects/Video";
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import DefaultCover from "Images/DefaultCover.jpg";
import { formatTime, formatDuration, formatCount } from "Components/Formatter";
import { ChangeVideoStatusMessage } from "Plugins/VideoService/APIs/ChangeVideoStatusMessage";
import { DeleteVideoMessage } from "Plugins/VideoService/APIs/DeleteVideoMessage";
import { useTopSuccessToast } from "Components/TopSuccessToast/useTopSuccessToast";

const MemberOverview: React.FC = () => {
    const { navigateMemberTab } = useNavigateMember();
    const { navigateVideo } = useNavigateVideo();
    const userToken = useUserToken();
    const { userID } = useUserID();
    const [videos, setVideos] = useState<Video[]>([]);
    const [loading, setLoading] = useState(true);
    const { ToastComponent, showSuccess } = useTopSuccessToast();

    useEffect(() => {
        if (userToken && userID) {
            loadUserVideos();
        }
    }, [userToken, userID]);

    const loadUserVideos = async () => {
        if (!userToken || !userID) return;

        try {
            setLoading(true);
            const response = await new Promise<string>((resolve, reject) => {
                new QueryUserVideosMessage(userToken, userID).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const videosData = JSON.parse(response);
            setVideos(videosData);
        } catch (error) {
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取视频列表失败");
        } finally {
            setLoading(false);
        }
    };

    const getStatusColor = (status: string): string => {
        switch (status) {
            case VideoStatus.approved:
                return '#4caf50';
            case VideoStatus.pending:
                return '#ff9800';
            case VideoStatus.rejected:
                return '#f44336';
            case VideoStatus.uploading:
                return '#2196f3';
            case VideoStatus.private:
                return '#9e9e9e';
            default:
                return '#666';
        }
    };

    const handleEditVideo = (videoID: number) => {
        navigateMemberTab(MemberPageTab.edit, videoID);

    };

    const handleManageDanmaku = (videoID: number) => {
        navigateMemberTab(MemberPageTab.danmaku, videoID);
    };

    const handleManageComments = (videoID: number) => {
        navigateVideo(videoID);
    };

    const handleVideoClick = (videoID: number) => {
        navigateVideo(videoID);
    };

    const handleSetPrivacy = async (videoID: number, oldStatus: VideoStatus) => {
        const newStatus = oldStatus === VideoStatus.private ? VideoStatus.pending : VideoStatus.private;
        try {
            await new Promise<void>((resolve, reject) => {
                new ChangeVideoStatusMessage(userToken, videoID, newStatus).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });
            loadUserVideos();
            showSuccess(`视频已设为${newStatus === VideoStatus.private ? "私密" : "公开"}`);
        } catch (error) {
            console.error("修改视频隐私状态失败:", error);
            materialAlertError("操作失败", error instanceof Error ? error.message : "修改视频隐私状态失败");
        }
    };

    const handleDeleteVideo = async (videoID: number) => {
        try {
            await new Promise<void>((resolve, reject) => {
                new DeleteVideoMessage(userToken, videoID).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });
            loadUserVideos();
            showSuccess("视频删除成功");
        } catch (error) {
            console.error("删除视频失败:", error);
            materialAlertError("操作失败", error instanceof Error ? error.message : "删除视频失败");
        }
    }

    if (loading) {
        return (
            <div className="member-loading">
                <div>加载中...</div>
            </div>
        );
    }

    return (
        <div className="member-overview">
            {ToastComponent}
            <h1 className="member-page-title marg">内容管理</h1>

            {videos.length === 0 ? (
                <div className="member-empty-state">
                    <div className="member-empty-icon">📹</div>
                    <div className="member-empty-text">您还没有上传任何视频</div>
                    <button
                        className="member-upload-btn"
                        onClick={() => navigateMemberTab(MemberPageTab.upload)}
                    >
                        立即上传
                    </button>
                </div>
            ) : (
                <div className="member-video-list">
                    {videos.map((video) => (
                        <div key={video.videoID} className="member-video-item">
                            <div
                                className="member-video-cover-container"
                                onClick={() => handleVideoClick(video.videoID)}
                            >
                                <img
                                    src={video.cover || DefaultCover}
                                    alt={video.title}
                                    className="member-video-cover"
                                />
                                <div
                                    className="member-video-status"
                                    style={{ backgroundColor: getStatusColor(video.status) }}
                                >
                                    {video.status}
                                </div>
                                {video.duration &&
                                    <div className="member-video-duration">
                                        {formatDuration(video.duration)}
                                    </div>}
                            </div>

                            <div className="member-video-info">
                                <div
                                    className="member-video-title"
                                    onClick={() => handleVideoClick(video.videoID)}
                                >{video.title}</div>
                                <div className="member-video-stats">
                                    <span>播放量: {formatCount(video.views)}</span>
                                    <span>点赞量: {formatCount(video.likes)}</span>
                                    <span>上传时间: {formatTime(video.uploadTime)}</span>
                                </div>
                            </div>

                            <div className="member-video-actions">
                                <button
                                    className="member-action-btn"
                                    onClick={() => handleEditVideo(video.videoID)}
                                >
                                    编辑视频
                                </button>
                                <button
                                    className="member-action-btn"
                                    onClick={() => handleManageDanmaku(video.videoID)}
                                >
                                    管理弹幕
                                </button>
                                <button
                                    className="member-action-btn"
                                    onClick={() => handleManageComments(video.videoID)}
                                >
                                    管理评论
                                </button>
                                <button
                                    className="member-action-btn"
                                    onClick={() => handleSetPrivacy(video.videoID, video.status)}
                                    disabled={video.status === VideoStatus.uploading || video.status === VideoStatus.broken}
                                >
                                    {(() => {
                                        switch (video.status) {
                                            case VideoStatus.private:
                                                return "设为公开";
                                            case VideoStatus.uploading:
                                                return "上传中..";
                                            case VideoStatus.broken:
                                                return "上传失败";
                                            default:
                                                return "设为私密";
                                        }
                                    })()}
                                </button>
                                <button
                                    className="member-action-btn"
                                    onClick={() => handleDeleteVideo(video.videoID)}
                                >
                                    删除视频
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default MemberOverview;
