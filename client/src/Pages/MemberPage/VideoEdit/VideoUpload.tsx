import React, { useState, useRef } from "react";
import axios from "axios";
import { useUserToken } from "Globals/GlobalStore";
import { UploadPath } from "Plugins/VideoService/Objects/UploadPath";
import { QueryUploadVideoPathMessage } from "Plugins/VideoService/APIs/QueryUploadVideoPathMessage";
import { ValidateVideoMessage } from "Plugins/VideoService/APIs/ValidateVideoMessage";
import { sendCover } from "./CoverUpload";
import { extractFirstFrameFromVideo } from "./VideoFrameExtractor";

const VideoUpload: React.FC<{
    isCreating: boolean;
    videoID: number;
    onVideoUploaded?: () => void;
}> = ({
    isCreating,
    videoID,
    onVideoUploaded
}) => {
    const userToken = useUserToken();
    const videoInputRef = useRef<HTMLInputElement>(null);
    const [videoUploading, setVideoUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [isSuccess, setIsSuccess] = useState(false);
    const [message, setMessage] = useState("");

    const setErrorMessage = (msg: string) => {
        setIsSuccess(false);
        setMessage(msg);
    };

    const setSuccessMessage = (msg: string) => {
        setIsSuccess(true);
        setMessage(msg);
    };

    const handleVideoUpload = async (file: File) => {
        // 验证文件类型
        const allowedTypes = ['video/mp4', 'video/webm', 'video/ogg', 'video/avi', 'video/mov'];
        if (!allowedTypes.includes(file.type)) {
            setErrorMessage("请选择 MP4、WebM、OGG、AVI 或 MOV 格式的视频文件");
            return;
        }

        // 验证文件大小 (500MB)
        const maxSize = 500 * 1024 * 1024;
        if (file.size > maxSize) {
            setErrorMessage("视频文件大小不能超过 500MB");
            return;
        }

        setVideoUploading(true);
        setUploadProgress(0);
        setMessage(""); // 清除之前的消息

        try {
            const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB
            const totalChunks = Math.ceil(file.size / CHUNK_SIZE);

            const uploadPath = await new Promise<UploadPath>((resolve, reject) => {
                new QueryUploadVideoPathMessage(userToken, videoID, totalChunks).send(
                    (info: string) => { resolve(JSON.parse(info) as UploadPath); },
                    (error: string) => { reject(new Error(error)); }
                );
            });

            const uploadUrl = uploadPath.path;
            const sessionToken = uploadPath.token;

            const uploadPart = async (fileChunk: Blob, presignedUrl: string): Promise<string> => {
                const response = await axios.put(presignedUrl, fileChunk, {
                    headers: { 'Content-Type': 'application/octet-stream' },
                });
                return response.headers.etag.replace(/"/g, ''); // 去掉引号
            };

            // 使用计数器来跟踪完成的块数量
            let completedChunks = 0;
            const etags: string[] = await Promise.all(uploadUrl.map(async (url, i) => {
                const start = i * CHUNK_SIZE;
                const end = Math.min(file.size, start + CHUNK_SIZE);
                const chunk = file.slice(start, end);
                const etag = await uploadPart(chunk, url);

                // 原子性地增加计数器并更新进度
                completedChunks++;
                setUploadProgress((completedChunks / totalChunks) * 90);

                return etag;
            }));

            await new Promise<void>((resolve, reject) => {
                new ValidateVideoMessage(sessionToken, etags).send(
                    (info: string) => { resolve(); },
                    (error: string) => { reject(new Error(error)); }
                );
            });

            if (isCreating) {
                // 自动从视频第一帧生成封面
                try {
                    console.log('开始自动生成封面');
                    setUploadProgress(95);
                    const frameFile = await extractFirstFrameFromVideo(file);
                    console.log("封面文件生成完成");
                    await sendCover(userToken, videoID, frameFile);
                } catch (coverError) {
                    console.warn("自动生成封面失败:", coverError);
                    // 封面生成失败不影响视频上传成功
                }
            }

            setUploadProgress(100);
            setSuccessMessage(isCreating ? "视频上传成功" : "视频上传成功，请等待审核");

            if (onVideoUploaded) {
                onVideoUploaded();
            }

        } catch (error) {
            const errorMsg = error instanceof Error ? error.message : "视频上传失败";
            setErrorMessage(errorMsg);
        } finally {
            setVideoUploading(false);
        }
    };

    const handleVideoFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            handleVideoUpload(file);
        }
        // 清空 input 值，确保下次选择相同文件时也能触发 onChange
        e.target.value = '';
    };

    const handleClick = () => {
        if (!videoUploading) {
            videoInputRef.current?.click();
        }
    };

    return (
        <div className="member-edit-tab-content">
            <div className="member-video-upload-section">
                {!videoUploading ? (
                    <div
                        className="member-upload-area"
                        onClick={handleClick}
                    >
                        <div className="member-upload-icon">📹</div>
                        <div className="member-upload-text">
                            {"也可拖拽上传"}
                        </div>
                        <button className="member-upload-btn" disabled={videoUploading}>
                            {isCreating ? "上传视频" : "选择视频"}
                        </button>
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

            {message && (
                isSuccess ? (
                    <div className="member-success-message">
                        <div className="member-success-icon">✓</div>
                        <div className="member-message-text">{message}</div>
                    </div>
                ) : (
                    <div className="member-error-message">
                        <div className="member-error-icon">!</div>
                        <div className="member-message-text">{message}</div>
                    </div>
                )
            )}

            <input
                ref={videoInputRef}
                type="file"
                accept="video/*"
                multiple={false}
                style={{ display: 'none' }}
                onChange={handleVideoFileSelect}
            />
        </div>
    );
};

export default VideoUpload;
