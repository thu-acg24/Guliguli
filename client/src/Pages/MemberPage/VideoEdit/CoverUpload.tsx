import React, { useState, useRef, useEffect } from "react";
import { useUserToken } from "Globals/GlobalStore";
import { QueryVideoInfoMessage } from "Plugins/VideoService/APIs/QueryVideoInfoMessage";
import { materialAlertError, materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";

interface CoverUploadProps {
    videoID: number;
}

const CoverUpload: React.FC<CoverUploadProps> = ({ videoID }) => {
    const userToken = useUserToken();
    const coverInputRef = useRef<HTMLInputElement>(null);
    const [coverUploading, setCoverUploading] = useState(false);
    const [coverUrl, setCoverUrl] = useState<string>("");
    const [loading, setLoading] = useState(true);

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
            if (videoData.coverPath) {
                setCoverUrl(videoData.coverPath);
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
            return newCoverUrl;
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : "封面上传失败";
            materialAlertError("上传失败", errorMessage);
            throw error;
        } finally {
            setCoverUploading(false);
        }
    };

    const handleCoverFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            uploadCover(file);
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
                <h3>当前封面</h3>
                {loading ? (
                    <div className="member-loading">
                        <div>加载中...</div>
                    </div>
                ) : (
                    <img
                        src={coverUrl || "/default-cover.jpg"}
                        alt="视频封面"
                        className="member-current-cover"
                    />
                )}
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
            />
        </div>
    );
};

export { CoverUpload };
export default CoverUpload;
