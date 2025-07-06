import React, { useState, useRef } from "react";
import { materialAlertError, materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";

interface VideoUploadProps {
    isCreating: boolean;
    videoID: number;
    onVideoUploaded?: () => void;
}

const VideoUpload: React.FC<VideoUploadProps> = ({
    isCreating,
    videoID,
    onVideoUploaded
}) => {
    const videoInputRef = useRef<HTMLInputElement>(null);
    const [videoUploading, setVideoUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);

    const handleVideoUpload = async (file: File) => {
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
            await new Promise(resolve => setTimeout(resolve, 3000));

            clearInterval(progressInterval);
            setUploadProgress(100);

            // 如果是创建视频，设置默认封面
            if (isCreating) {
                try {
                    // 创建一个虚拟的封面文件（实际应该从视频第一帧生成）
                    const canvas = document.createElement('canvas');
                    canvas.width = 320;
                    canvas.height = 180;
                    const ctx = canvas.getContext('2d');
                    if (ctx) {
                        ctx.fillStyle = '#f0f0f0';
                        ctx.fillRect(0, 0, canvas.width, canvas.height);
                        ctx.fillStyle = '#666';
                        ctx.font = '16px Arial';
                        ctx.textAlign = 'center';
                        ctx.fillText('视频封面', canvas.width / 2, canvas.height / 2);
                    }

                    canvas.toBlob(async (blob) => {
                        if (blob) {
                            // 设置默认封面
                            const defaultCoverUrl = "/api/video/cover/" + videoID;
                            // 封面设置逻辑由 CoverUpload 组件内部处理
                        }
                    }, 'image/jpeg');
                } catch (error) {
                    console.warn("设置默认封面失败:", error);
                }
            }

            setTimeout(() => {
                materialAlertSuccess("视频上传成功", isCreating ? "请设置视频封面" : "新视频已上传");
                if (onVideoUploaded) {
                    onVideoUploaded();
                }
            }, 1000);
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

    const handleClick = () => {
        if (!videoUploading) {
            videoInputRef.current?.click();
        }
    };

    return (
        <div className="member-edit-tab-content">
            <div className="member-video-upload-section">
                <h3>{isCreating ? "上传视频" : "更换视频"}</h3>
                {!videoUploading ? (
                    <div
                        className="member-upload-area"
                        onClick={handleClick}
                    >
                        <div className="member-upload-icon">📹</div>
                        <div className="member-upload-text">
                            {isCreating ? "拖拽到此处也可上传" : "点击选择新视频文件"}
                        </div>
                        <button className="member-upload-btn" disabled={videoUploading}>
                            {isCreating ? "上传视频" : "选择视频"}
                        </button>
                        {isCreating && (
                            <div className="member-upload-audit-progress">
                                <span>当前审核队列</span>
                                <span className="tag" style={{ backgroundColor: "#4581B6" }}>
                                    快速 <span className="tag-block">预计审核完成时间：10分钟内</span>
                                </span>
                            </div>
                        )}
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
};

export default VideoUpload;
