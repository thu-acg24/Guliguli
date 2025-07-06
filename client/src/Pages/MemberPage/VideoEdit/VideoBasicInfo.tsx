import React, { useState, useEffect } from "react";
import { useUserToken } from "Globals/GlobalStore";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";

interface VideoBasicInfoProps {
    isCreating: boolean;
    videoID?: number;
    loadVideoInfo?: () => Promise<{ title: string; description: string; tags: string[] }>;
    onSubmit: (title: string, description: string, tags: string[]) => Promise<void>;
}

const VideoBasicInfo: React.FC<VideoBasicInfoProps> = ({
    isCreating,
    videoID,
    loadVideoInfo,
    onSubmit
}) => {
    const userToken = useUserToken();
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [tags, setTags] = useState<string[]>([]);
    const [tagInput, setTagInput] = useState("");
    const [isSuccess, setIsSuccess] = useState(false);
    const [message, setMessage] = useState("");

    useEffect(() => {
        if (!isCreating && loadVideoInfo) {
            loadInitialInfo();
        }
    }, [isCreating, loadVideoInfo]);

    const loadInitialInfo = async () => {
        if (!loadVideoInfo) return;

        try {
            setLoading(true);
            setMessage(""); // 清除之前的消息
            const videoInfo = await loadVideoInfo();
            setTitle(videoInfo.title);
            setDescription(videoInfo.description);
            setTags(videoInfo.tags);
        } catch (error) {
            const errorMsg = error instanceof Error ? error.message : "获取视频信息失败";
            setErrorMessage(errorMsg);
            materialAlertError("加载失败", errorMsg);
        } finally {
            setLoading(false);
        }
    };

    const setErrorMessage = (msg: string) => {
        setIsSuccess(false);
        setMessage(msg);
    };

    const setSuccessMessage = (msg: string) => {
        setIsSuccess(true);
        setMessage(msg);
    };

    const handleAddTag = () => {
        const trimmedTag = tagInput.trim();
        if (!trimmedTag) {
            setErrorMessage("标签不能为空");
            return;
        }
        if (tags.includes(trimmedTag)) {
            setErrorMessage("标签已存在");
            return;
        }
        if (tags.length >= 10) {
            setErrorMessage("不能超过10个标签");
            return;
        }
        setTags([...tags, trimmedTag]);
        setTagInput("");
    };

    const handleRemoveTag = (tagToRemove: string) => {
        setTags(tags.filter(tag => tag !== tagToRemove));
    };

    const handleTagInputKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleAddTag();
        }
    };

    const handleSubmit = async () => {
        if (!userToken) {
            materialAlertError("未登录", "请先登录");
            return;
        }

        if (!title.trim()) {
            const errorMsg = "请输入视频标题";
            setErrorMessage(errorMsg);
            console.error("标题不能为空", errorMsg);
            return;
        }

        if (!description.trim()) {
            const errorMsg = "请输入视频简介";
            setErrorMessage(errorMsg);
            console.error("简介不能为空", errorMsg);
            return;
        }

        try {
            setSaving(true);
            setMessage(""); // 清除之前的消息
            await onSubmit(title, description, tags);
            setSuccessMessage(isCreating ? "视频创建成功" : "基本信息保存成功");
        } catch (error) {
            materialAlertError("操作失败", error instanceof Error ? error.message : "操作失败");
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="member-loading">
                <div>加载中...</div>
            </div>
        );
    }

    return (
        <div className="member-edit-tab-content">

            <div className="member-form-group">
                <label htmlFor="title">视频标题</label>
                <input
                    type="text"
                    id="title"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="请输入视频标题"
                    maxLength={30}
                />
            </div>

            <div className="member-form-group">
                <label htmlFor="description">视频简介</label>
                <textarea
                    id="description"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="请输入视频简介"
                    rows={4}
                    maxLength={500}
                />
            </div>

            <div className="member-form-group">
                <label htmlFor="tags">标签</label>
                <div className="member-tag-input-container">
                    {tags.length > 0 && (
                        <div className="member-tag-list">
                            {tags.map((tag, index) => (
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
                        maxLength={15}
                    />
                </div>
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

            <button
                className="member-form-submit"
                onClick={handleSubmit}
                disabled={saving}
            >
                {saving ? (isCreating ? "创建中..." : "保存中...") : (isCreating ? "创建视频" : "保存基本信息")}
            </button>
        </div>
    );
};

export default VideoBasicInfo;
