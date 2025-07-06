import React, { useState, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { useUserToken } from "Globals/GlobalStore";
import { UploadVideoMessage } from "Plugins/VideoService/APIs/UploadVideoMessage";
import { materialAlertError, materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { memberPagePath } from "./MemberPage";

interface VideoInfo {
    title: string;
    description: string;
    tags: string[];
}

const MemberUpload: React.FC = () => {
    const navigate = useNavigate();
    const userToken = useUserToken();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const coverInputRef = useRef<HTMLInputElement>(null);

    const [step, setStep] = useState(1); // 1: 基本信息, 2: 上传视频, 3: 设置封面
    const [videoInfo, setVideoInfo] = useState<VideoInfo>({
        title: "",
        description: "",
        tags: []
    });
    const [videoID, setVideoID] = useState<number | null>(null);
    const [tagInput, setTagInput] = useState("");
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [coverUrl, setCoverUrl] = useState<string>("");
    const [coverUploading, setCoverUploading] = useState(false);

    const handleVideoInfoChange = (field: keyof VideoInfo, value: any) => {
        setVideoInfo(prev => ({
            ...prev,
            [field]: value
        }));
    };

    const handleAddTag = () => {
        const trimmedTag = tagInput.trim();
        if (trimmedTag && !videoInfo.tags.includes(trimmedTag)) {
            handleVideoInfoChange("tags", [...videoInfo.tags, trimmedTag]);
            setTagInput("");
        }
    };

    const handleRemoveTag = (tagToRemove: string) => {
        handleVideoInfoChange("tags", videoInfo.tags.filter(tag => tag !== tagToRemove));
    };

    const handleTagInputKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleAddTag();
        }
    };

    const handleCreateVideo = async () => {
        if (!userToken) {
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
            setUploading(true);
            const response = await new Promise<string>((resolve, reject) => {
                new UploadVideoMessage(
                    userToken,
                    videoInfo.title,
                    videoInfo.description,
                    videoInfo.tags
                ).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const createdVideoID = parseInt(response);
            setVideoID(createdVideoID);
            setStep(2);
            materialAlertSuccess("视频创建成功", "请继续上传视频文件");
        } catch (error) {
            materialAlertError("创建失败", error instanceof Error ? error.message : "创建视频失败");
        } finally {
            setUploading(false);
        }
    };

    const handleVideoUpload = async (file: File) => {
        if (!videoID) return;

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

        setUploading(true);
        setUploadProgress(0);

        try {
            // 模拟上传进度
            const progressInterval = setInterval(() => {
                setUploadProgress(prev => {
                    if (prev >= 90) {
                        clearInterval(progressInterval);
                        return prev;
                    }
                    return prev + Math.random() * 10;
                });
            }, 500);

            // 实际上传逻辑
            const formData = new FormData();
            formData.append('video', file);
            formData.append('videoID', videoID.toString());

            // 这里应该调用实际的上传 API
            await new Promise(resolve => setTimeout(resolve, 3000)); // 模拟上传时间

            clearInterval(progressInterval);
            setUploadProgress(100);

            // 设置默认封面（实际应该从后端获取）
            setCoverUrl("/api/video/cover/" + videoID);

            setTimeout(() => {
                setStep(3);
                materialAlertSuccess("视频上传成功", "请设置视频封面");
            }, 1000);
        } catch (error) {
            materialAlertError("上传失败", error instanceof Error ? error.message : "视频上传失败");
        } finally {
            setUploading(false);
        }
    };

    const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            handleVideoUpload(file);
        }
    };

    const handleCoverUpload = async (file: File) => {
        if (!videoID) return;

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
            formData.append('videoID', videoID.toString());

            // 这里应该调用实际的封面上传 API
            await new Promise(resolve => setTimeout(resolve, 1000));

            // 更新封面预览
            const newCoverUrl = URL.createObjectURL(file);
            setCoverUrl(newCoverUrl);

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

    const handleFinish = () => {
        materialAlertSuccess("视频上传完成", "您的视频已成功上传，等待审核");
        navigate(memberPagePath);
    };

    const renderStep1 = () => (
        <div className="member-upload-step-content">
            <h2>基本信息</h2>
            <div className="member-form-group">
                <label htmlFor="title">视频标题</label>
                <input
                    type="text"
                    id="title"
                    value={videoInfo.title}
                    onChange={(e) => handleVideoInfoChange("title", e.target.value)}
                    placeholder="请输入视频标题"
                    maxLength={100}
                />
            </div>

            <div className="member-form-group">
                <label htmlFor="description">视频简介</label>
                <textarea
                    id="description"
                    value={videoInfo.description}
                    onChange={(e) => handleVideoInfoChange("description", e.target.value)}
                    placeholder="请输入视频简介"
                    rows={4}
                    maxLength={500}
                />
            </div>

            <div className="member-form-group">
                <label htmlFor="tags">标签</label>
                <div className="member-tag-input-container">
                    {videoInfo.tags.length > 0 && (
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
                onClick={handleCreateVideo}
                disabled={uploading}
            >
                {uploading ? "创建中..." : "创建视频"}
            </button>
        </div>
    );

    const renderStep2 = () => (
        <div className="member-upload-step-content">
            <h2>上传视频</h2>
            {!uploading ? (
                <div
                    className="member-upload-area"
                    onClick={() => fileInputRef.current?.click()}
                >
                    <div className="member-upload-icon">📹</div>
                    <div className="member-upload-text">拖拽到此处也可上传</div>
                    <button className="member-upload-btn">上传视频</button>
                    <div className="member-upload-audit-progress">
                        <span>当前审核队列</span>
                        <span className="tag" style={{ backgroundColor: "#4581B6" }}>
                            快速 <span className="tag-block">预计审核完成时间：10分钟内</span>
                        </span>
                    </div>
                </div>
            ) : (
                <div className="member-upload-progress">
                    <div className="member-upload-progress-bar">
                        <div
                            className="member-upload-progress-fill"
                            style={{ width: `${uploadProgress}%` }}
                        />
                    </div>
                    <div className="member-upload-progress-text">
                        {uploadProgress < 100 ? `上传中... ${uploadProgress.toFixed(1)}%` : "视频处理中..."}
                    </div>
                </div>
            )}
            <input
                ref={fileInputRef}
                type="file"
                accept="video/*"
                style={{ display: 'none' }}
                onChange={handleFileSelect}
            />
        </div>
    );

    const renderStep3 = () => (
        <div className="member-upload-step-content">
            <h2>设置封面</h2>
            <div className="member-cover-setting">
                <img
                    src={coverUrl || "/default-cover.jpg"}
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
                    <button
                        className="member-cover-upload-btn"
                        onClick={handleFinish}
                        disabled={coverUploading}
                    >
                        完成上传
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

    return (
        <div className="member-upload-container">
            <h1 className="member-upload-title">上传视频</h1>

            <div className="member-upload-steps">
                <div className={`member-upload-step ${step >= 1 ? 'active' : ''} ${step > 1 ? 'completed' : ''}`}>
                    <div className="member-upload-step-number">1</div>
                    <div className="member-upload-step-text">基本信息</div>
                </div>
                <div className={`member-upload-step ${step >= 2 ? 'active' : ''} ${step > 2 ? 'completed' : ''}`}>
                    <div className="member-upload-step-number">2</div>
                    <div className="member-upload-step-text">上传视频</div>
                </div>
                <div className={`member-upload-step ${step >= 3 ? 'active' : ''} ${step > 3 ? 'completed' : ''}`}>
                    <div className="member-upload-step-number">3</div>
                    <div className="member-upload-step-text">设置封面</div>
                </div>
            </div>

            {step === 1 && renderStep1()}
            {step === 2 && renderStep2()}
            {step === 3 && renderStep3()}
        </div>
    );
};

export default MemberUpload;
