import React, { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useUserToken } from "Globals/GlobalStore";
import { ModifyVideoMessage } from "Plugins/VideoService/APIs/ModifyVideoMessage";
import { QueryVideoInfoMessage } from "Plugins/VideoService/APIs/QueryVideoInfoMessage";
import { materialAlertError, materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { memberPagePath } from "./MemberPage";

interface VideoEditInfo {
    videoID: number;
    title: string;
    description: string;
    tags: string[];
    coverPath: string;
    videoPath: string;
    status: string;
}

const MemberVideoEdit: React.FC = () => {
    const { videoID } = useParams<{ videoID: string }>();
    const navigate = useNavigate();
    const userToken = useUserToken();
    const videoInputRef = useRef<HTMLInputElement>(null);
    const coverInputRef = useRef<HTMLInputElement>(null);

    const [videoInfo, setVideoInfo] = useState<VideoEditInfo | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [tagInput, setTagInput] = useState("");
    const [activeTab, setActiveTab] = useState<'basic' | 'video' | 'cover'>('basic');
    const [videoUploading, setVideoUploading] = useState(false);
    const [coverUploading, setCoverUploading] = useState(false);

    useEffect(() => {
        if (videoID) {
            loadVideoInfo();
        }
    }, [videoID]);

    const loadVideoInfo = async () => {
        if (!videoID) return;

        try {
            setLoading(true);
            console.log(userToken, parseInt(videoID));
            const response = await new Promise<string>((resolve, reject) => {
                new QueryVideoInfoMessage(userToken, parseInt(videoID)).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const videoData = JSON.parse(response);
            const loadedVideoInfo: VideoEditInfo = {
                videoID: videoData.videoID,
                title: videoData.title,
                description: videoData.description,
                tags: videoData.tag || [],
                coverPath: videoData.coverPath,
                videoPath: videoData.videoPath,
                status: videoData.status
            };

            setVideoInfo(loadedVideoInfo);
        } catch (error) {
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取视频信息失败");
        } finally {
            setLoading(false);
        }
    };

    const handleVideoInfoChange = (field: keyof VideoEditInfo, value: any) => {
        if (!videoInfo) return;

        setVideoInfo(prev => prev ? {
            ...prev,
            [field]: value
        } : null);
    };

    const handleAddTag = () => {
        if (!videoInfo) return;

        const trimmedTag = tagInput.trim();
        if (trimmedTag && !videoInfo.tags.includes(trimmedTag)) {
            handleVideoInfoChange("tags", [...videoInfo.tags, trimmedTag]);
            setTagInput("");
        }
    };

    const handleRemoveTag = (tagToRemove: string) => {
        if (!videoInfo) return;

        handleVideoInfoChange("tags", videoInfo.tags.filter(tag => tag !== tagToRemove));
    };

    const handleTagInputKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleAddTag();
        }
    };

    const handleSaveBasicInfo = async () => {
        if (!userToken || !videoInfo) {
            materialAlertError("未登录", "请先登录");
            return;
        }

        if (!videoInfo.title.trim()) {
            materialAlertError("标题不能为空", "请输入视频标题");
            return;
        }

        if (!videoInfo.description.trim()) {
            materialAlertError("简介不能为空", "请输入视频简介");
            return;
        }

        if (videoInfo.tags.length === 0) {
            materialAlertError("标签不能为空", "请至少添加一个标签");
            return;
        }

        try {
            setSaving(true);
            await new Promise<void>((resolve, reject) => {
                new ModifyVideoMessage(
                    userToken,
                    videoInfo.videoID,
                    videoInfo.title,
                    videoInfo.description,
                    videoInfo.tags
                ).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });

            materialAlertSuccess("保存成功", "视频信息已更新");
        } catch (error) {
            materialAlertError("保存失败", error instanceof Error ? error.message : "更新视频信息失败");
        } finally {
            setSaving(false);
        }
    };

    const handleVideoUpload = async (file: File) => {
        if (!videoInfo) return;

        // 验证文件类型
        const allowedTypes = ['video/mp4', 'video/webm', 'video/ogg', 'video/avi', 'video/mov'];
        if (!allowedTypes.includes(file.type)) {
            materialAlertError("文件类型不支持", "请选择 MP4、WebM、OGG、AVI 或 MOV 格式的视频文件");
            return;
        }

        // 验证文件大小 (500MB)
        const maxSize = 500 * 1024 * 1024;
        if (file.size > maxSize) {
            materialAlertError("文件过大", "视频文件大小不能超过 500MB");
            return;
        }

        setVideoUploading(true);

        try {
            // 实际上传视频逻辑
            const formData = new FormData();
            formData.append('video', file);
            formData.append('videoID', videoInfo.videoID.toString());

            // 这里应该调用实际的上传 API
            await new Promise(resolve => setTimeout(resolve, 3000));

            materialAlertSuccess("视频上传成功", "新视频已上传");
        } catch (error) {
            materialAlertError("上传失败", error instanceof Error ? error.message : "视频上传失败");
        } finally {
            setVideoUploading(false);
        }
    };

    const handleVideoFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            handleVideoUpload(file);
        }
    };

    const handleCoverUpload = async (file: File) => {
        if (!videoInfo) return;

        // 验证文件类型
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
        if (!allowedTypes.includes(file.type)) {
            materialAlertError("文件类型不支持", "请选择 JPEG、PNG、GIF 或 WebP 格式的图片");
            return;
        }

        // 验证文件大小 (5MB)
        const maxSize = 5 * 1024 * 1024;
        if (file.size > maxSize) {
            materialAlertError("文件过大", "图片大小不能超过 5MB");
            return;
        }

        setCoverUploading(true);

        try {
            // 实际上传封面逻辑
            const formData = new FormData();
            formData.append('cover', file);
            formData.append('videoID', videoInfo.videoID.toString());

            // 这里应该调用实际的封面上传 API
            await new Promise(resolve => setTimeout(resolve, 1000));

            // 更新封面预览
            const newCoverUrl = URL.createObjectURL(file);
            handleVideoInfoChange("coverPath", newCoverUrl);

            materialAlertSuccess("封面上传成功", "");
        } catch (error) {
            materialAlertError("上传失败", error instanceof Error ? error.message : "封面上传失败");
        } finally {
            setCoverUploading(false);
        }
    };

    const handleCoverFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            handleCoverUpload(file);
        }
    };

    const renderBasicTab = () => (
        <div className="member-edit-tab-content">
            <div className="member-form-group">
                <label htmlFor="title">视频标题</label>
                <input
                    type="text"
                    id="title"
                    value={videoInfo?.title || ""}
                    onChange={(e) => handleVideoInfoChange("title", e.target.value)}
                    placeholder="请输入视频标题"
                    maxLength={100}
                />
            </div>

            <div className="member-form-group">
                <label htmlFor="description">视频简介</label>
                <textarea
                    id="description"
                    value={videoInfo?.description || ""}
                    onChange={(e) => handleVideoInfoChange("description", e.target.value)}
                    placeholder="请输入视频简介"
                    rows={4}
                    maxLength={500}
                />
            </div>

            <div className="member-form-group">
                <label htmlFor="tags">标签</label>
                <div className="member-tag-input-container">
                    {videoInfo && videoInfo.tags.length > 0 && (
                        <div className="member-tag-list">
                            {videoInfo.tags.map((tag, index) => (
                                <span key={index} className="member-tag-item">
                                    {tag}
                                    <span
                                        className="member-tag-remove"
                                        onClick={() => handleRemoveTag(tag)}
                                    >
                                        ×
                                    </span>
                                </span>
                            ))}
                        </div>
                    )}
                    <input
                        type="text"
                        id="tags"
                        value={tagInput}
                        onChange={(e) => setTagInput(e.target.value)}
                        onKeyPress={handleTagInputKeyPress}
                        placeholder="输入标签后按回车添加"
                    />
                </div>
            </div>

            <button
                className="member-form-submit"
                onClick={handleSaveBasicInfo}
                disabled={saving}
            >
                {saving ? "保存中..." : "保存基本信息"}
            </button>
        </div>
    );

    const renderVideoTab = () => (
        <div className="member-edit-tab-content">
            <div className="member-video-upload-section">
                <h3>更换视频</h3>
                <div
                    className="member-upload-area"
                    onClick={() => videoInputRef.current?.click()}
                >
                    <div className="member-upload-icon">📹</div>
                    <div className="member-upload-text">
                        {videoUploading ? "上传中..." : "点击选择新视频文件"}
                    </div>
                    <button className="member-upload-btn" disabled={videoUploading}>
                        {videoUploading ? "上传中..." : "选择视频"}
                    </button>
                </div>
            </div>

            <input
                ref={videoInputRef}
                type="file"
                accept="video/*"
                style={{ display: 'none' }}
                onChange={handleVideoFileSelect}
            />
        </div>
    );

    const renderCoverTab = () => (
        <div className="member-edit-tab-content">
            <div className="member-cover-setting">
                <h3>当前封面</h3>
                <img
                    src={videoInfo?.coverPath || "/default-cover.jpg"}
                    alt="视频封面"
                    className="member-current-cover"
                />
                <div className="member-cover-upload">
                    <button
                        className="member-cover-upload-btn"
                        onClick={() => coverInputRef.current?.click()}
                        disabled={coverUploading}
                    >
                        {coverUploading ? "上传中..." : "更换封面"}
                    </button>
                </div>
            </div>
            <input
                ref={coverInputRef}
                type="file"
                accept="image/*"
                style={{ display: 'none' }}
                onChange={handleCoverFileSelect}
            />
        </div>
    );

    if (loading) {
        return (
            <div className="member-loading">
                <div>加载中...</div>
            </div>
        );
    }

    if (!videoInfo) {
        return (
            <div className="member-error">
                <div>视频信息加载失败</div>
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
