import React, { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useUserToken } from "Globals/GlobalStore";
import { ModifyVideoMessage } from "Plugins/VideoService/APIs/ModifyVideoMessage";
import { QueryVideoInfoMessage } from "Plugins/VideoService/APIs/QueryVideoInfoMessage";
import { materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { memberPagePath } from "./MemberPage";
import { VideoBasicInfo, VideoUpload, CoverUpload } from "./VideoEdit";

const MemberVideoEdit: React.FC = () => {
    const { videoID } = useParams<{ videoID: string }>();
    const navigate = useNavigate();
    const userToken = useUserToken();

    const [activeTab, setActiveTab] = useState<'basic' | 'video' | 'cover'>('basic');

    const loadVideoInfoForComponent = async () => {
        if (!videoID) return { title: "", description: "", tags: [] };

        try {
            const response = await new Promise<string>((resolve, reject) => {
                new QueryVideoInfoMessage(userToken, parseInt(videoID)).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const videoData = JSON.parse(response);

            return {
                title: videoData.title || "",
                description: videoData.description || "",
                tags: videoData.tag || []
            };
        } catch (error) {
            throw new Error(error instanceof Error ? error.message : "获取视频信息失败");
        }
    };

    const handleSaveBasicInfo = async (title: string, description: string, tags: string[]) => {
        if (!userToken || !videoID) {
            throw new Error("未登录或视频ID无效");
        }

        await new Promise<void>((resolve, reject) => {
            new ModifyVideoMessage(
                userToken,
                parseInt(videoID),
                title,
                description,
                tags
            ).send(
                (info: string) => resolve(),
                (error: string) => reject(new Error(error))
            );
        });

        materialAlertSuccess("保存成功", "视频信息已更新");
    };

    const renderBasicTab = () => (
        <VideoBasicInfo
            isCreating={false}
            videoID={videoID ? parseInt(videoID) : undefined}
            loadVideoInfo={loadVideoInfoForComponent}
            onSubmit={handleSaveBasicInfo}
        />
    );

    const renderVideoTab = () => (
        <VideoUpload
            isCreating={false}
            videoID={videoID ? parseInt(videoID) : 0}
        />
    );

    const renderCoverTab = () => (
        <CoverUpload
            videoID={videoID ? parseInt(videoID) : 0}
        />
    );

    if (!videoID) {
        return (
            <div className="member-error">
                <div>无效的视频ID</div>
            </div>
        );
    }

    return (
        <div className="member-video-edit">
            <div className="member-page-header">
                <button
                    className="member-back-btn"
                    onClick={() => navigate(memberPagePath)}
                >
                    ← 返回
                </button>
                <h1 className="member-page-title">编辑视频</h1>
            </div>

            <div className="member-edit-tabs">
                <div
                    className={`member-edit-tab ${activeTab === 'basic' ? 'active' : ''}`}
                    onClick={() => setActiveTab('basic')}
                >
                    基本设置
                </div>
                <div
                    className={`member-edit-tab ${activeTab === 'video' ? 'active' : ''}`}
                    onClick={() => setActiveTab('video')}
                >
                    视频文件
                </div>
                <div
                    className={`member-edit-tab ${activeTab === 'cover' ? 'active' : ''}`}
                    onClick={() => setActiveTab('cover')}
                >
                    封面设置
                </div>
            </div>

            <div className="member-edit-content">
                {activeTab === 'basic' && renderBasicTab()}
                {activeTab === 'video' && renderVideoTab()}
                {activeTab === 'cover' && renderCoverTab()}
            </div>
        </div>
    );
};

export default MemberVideoEdit;
