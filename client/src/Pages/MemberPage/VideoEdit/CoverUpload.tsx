import React, { useState, useRef, useEffect } from "react";
import { useUserToken } from "Globals/GlobalStore";
import { QueryVideoInfoMessage } from "Plugins/VideoService/APIs/QueryVideoInfoMessage";
import { UploadPath } from "Plugins/VideoService/Objects/UploadPath";
import { QueryUploadCoverPathMessage } from "Plugins/VideoService/APIs/QueryUploadCoverPathMessage";
import { ValidateCoverMessage } from "Plugins/VideoService/APIs/ValidateCoverMessage";
import DefaultCover from "Images/DefaultCover.jpg";

interface CoverUploadProps {
    videoID: number;
    refreshKey?: number;
}

export const sendCover = async (userToken: string, videoID: number, file: File): Promise<void> => {
    const uploadPath = await new Promise<UploadPath>((resolve, reject) => {
        new QueryUploadCoverPathMessage(userToken, videoID).send(
            (info: string) => { resolve(JSON.parse(info) as UploadPath); },
            (error) => { reject(new Error(error)); }
        );
    });
    const uploadUrl = uploadPath.path[0];
    const sessionToken = uploadPath.token;
    const uploadResponse = await fetch(uploadUrl, {
        method: 'PUT',
        body: file,
        headers: {
            'Content-Type': file.type,
        },
    });
    if (!uploadResponse.ok) {
        throw new Error('上传失败');
    }
    await new Promise<void>((resolve, reject) => {
        new ValidateCoverMessage(sessionToken).send(
            (info: string) => { resolve(); },
            (error) => { reject(new Error(error)); }
        );
    });
    await new Promise(resolve => setTimeout(resolve, 3000)); // 等待封面验证完成
    console.log("sendCover Success")
}

const CoverUpload: React.FC<CoverUploadProps> = ({ videoID, refreshKey }) => {
    const userToken = useUserToken();
    const coverInputRef = useRef<HTMLInputElement>(null);
    const [coverUploading, setCoverUploading] = useState(false);
    const [coverUrl, setCoverUrl] = useState<string>("");
    const [loading, setLoading] = useState(true);
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

    useEffect(() => {
        loadCurrentCover();
    }, [videoID]);

    const loadCurrentCover = async () => {
        if (!videoID) return;

        try {
            setLoading(true);
            const response = await new Promise<string>((resolve, reject) => {
                new QueryVideoInfoMessage(userToken, videoID).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const videoData = JSON.parse(response);
            if (videoData.cover) {
                setCoverUrl(videoData.cover);
            }
        } catch (error) {
            console.warn("获取封面信息失败:", error);
        } finally {
            setLoading(false);
        }
    };

    const uploadCover = async (file: File): Promise<string> => {
        // 验证文件类型
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
        if (!allowedTypes.includes(file.type)) {
            throw new Error("请选择 JPEG、PNG、GIF 或 WebP 格式的图片");
        }

        // 验证文件大小 (5MB)
        const maxSize = 5 * 1024 * 1024;
        if (file.size > maxSize) {
            throw new Error("图片大小不能超过 5MB");
        }

        setCoverUploading(true);
        setMessage(""); // 清除之前的消息

        try {
            await sendCover(userToken, videoID, file);
            // 更新封面预览
            const newCoverUrl = URL.createObjectURL(file);
            setCoverUrl(newCoverUrl);

            setSuccessMessage("封面上传成功");
            return newCoverUrl;
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : "封面上传失败";
            setErrorMessage(errorMessage);
            console.error("封面上传失败:", error);
        } finally {
            setCoverUploading(false);
        }
    };

    const handleCoverFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            uploadCover(file)
        }
    };

    const handleClick = () => {
        if (!coverUploading) {
            coverInputRef.current?.click();
        }
    };

    return (
        <div className="member-edit-tab-content">
            <div className="member-cover-setting">
                {loading ? (
                    <div className="member-loading">
                        <div>加载中...</div>
                    </div>
                ) : (
                    <img
                        src={coverUrl || DefaultCover}
                        alt="视频封面"
                        className="member-current-cover"
                    />
                )}
                <div className="member-short-message">
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
                </div>
                <div className="member-cover-upload">
                    <button
                        className="member-cover-upload-btn"
                        onClick={handleClick}
                        disabled={coverUploading || loading}
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
                multiple={false}
            />
        </div >
    );
};

export { CoverUpload };
export default CoverUpload;
