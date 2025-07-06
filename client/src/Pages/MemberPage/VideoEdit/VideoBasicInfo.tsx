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

    useEffect(() => {
        if (!isCreating && loadVideoInfo) {
            loadInitialInfo();
        }
    }, [isCreating, loadVideoInfo]);

    const loadInitialInfo = async () => {
        if (!loadVideoInfo) return;

        try {
            setLoading(true);
            const videoInfo = await loadVideoInfo();
            setTitle(videoInfo.title);
            setDescription(videoInfo.description);
            setTags(videoInfo.tags);
        } catch (error) {
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取视频信息失败");
        } finally {
            setLoading(false);
        }
    };

    const handleAddTag = () => {
        const trimmedTag = tagInput.trim();
        if (trimmedTag && !tags.includes(trimmedTag)) {
            setTags([...tags, trimmedTag]);
            setTagInput("");
        }
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
            materialAlertError("标题不能为空", "请输入视频标题");
            return;
        }

        if (!description.trim()) {
            materialAlertError("简介不能为空", "请输入视频简介");
            return;
        }

        if (tags.length === 0) {
            materialAlertError("标签不能为空", "请至少添加一个标签");
            return;
        }

        try {
            setSaving(true);
            await onSubmit(title, description, tags);
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
                    maxLength={100}
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
                    />
                </div>
            </div>

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
