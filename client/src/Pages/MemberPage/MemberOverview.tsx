import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { MemberPageTab, useNavigateMember, useNavigateVideo } from "Globals/Navigate";
import { useUserToken, useUserID } from "Globals/GlobalStore";
import { QueryUserVideosMessage } from "Plugins/VideoService/APIs/QueryUserVideosMessage";
import { Video } from "Plugins/VideoService/Objects/Video";
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus";
import { materialAlertError, materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import DefaultCover from "Images/DefaultCover.jpg";
import { formatDuration, formatCount } from "Components/Formatter";

const MemberOverview: React.FC = () => {
    const navigate = useNavigate();
    const { navigateMemberTab } = useNavigateMember();
    const { navigateVideo } = useNavigateVideo();
    const userToken = useUserToken();
    const { userID } = useUserID();
    const [videos, setVideos] = useState<Video[]>([]);
    const [loading, setLoading] = useState(true);

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

    if (loading) {
        return (
            <div className="member-loading">
                <div>加载中...</div>
            </div>
        );
    }

    return (
        <div className="member-overview">
            <h1 className="member-page-title">内容管理</h1>

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
                                    <span>上传时间: {new Date(video.uploadTime).toLocaleDateString()}</span>
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
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default MemberOverview;
