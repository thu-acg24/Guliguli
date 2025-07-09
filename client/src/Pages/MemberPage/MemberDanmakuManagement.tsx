import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useNavigateMember } from "Globals/Navigate";
import { useUserToken } from "Globals/GlobalStore";
import { QueryVideoDanmakuMessage } from "Plugins/DanmakuService/APIs/QueryVideoDanmakuMessage";
import { DeleteDanmakuMessage } from "Plugins/DanmakuService/APIs/DeleteDanmakuMessage";
import { materialAlertError, materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { formatTime, formatDuration } from "Components/Formatter";

interface Danmaku {
    danmakuID: number;
    content: string;
    time: number;
    color: string;
    user: {
        userID: number;
        username: string;
        avatarPath: string;
    };
    createdAt: string;
}

const MemberDanmakuManagement: React.FC = () => {
    const { videoID } = useParams<{ videoID: string }>();
    const { navigateMember } = useNavigateMember();
    const userToken = useUserToken();

    const [danmakus, setDanmakus] = useState<Danmaku[]>([]);
    const [loading, setLoading] = useState(true);
    const [deletingIds, setDeletingIds] = useState<Set<number>>(new Set());

    useEffect(() => {
        if (videoID) {
            loadDanmakus();
        }
    }, [videoID]);

    const loadDanmakus = async () => {
        if (!videoID) return;

        try {
            setLoading(true);
            const response = await new Promise<string>((resolve, reject) => {
                new QueryVideoDanmakuMessage(parseInt(videoID)).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const danmakusData = JSON.parse(response);
            setDanmakus(danmakusData);
        } catch (error) {
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取弹幕列表失败");
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteDanmaku = async (danmakuID: number) => {
        if (!userToken) {
            materialAlertError("未登录", "请先登录");
            return;
        }

        if (!confirm("确定要删除这条弹幕吗？")) {
            return;
        }

        try {
            setDeletingIds(prev => new Set(prev).add(danmakuID));

            await new Promise<void>((resolve, reject) => {
                new DeleteDanmakuMessage(userToken, danmakuID).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });

            // 从列表中移除已删除的弹幕
            setDanmakus(prev => prev.filter(danmaku => danmaku.danmakuID !== danmakuID));
            materialAlertSuccess("删除成功", "弹幕已被删除");
        } catch (error) {
            materialAlertError("删除失败", error instanceof Error ? error.message : "删除弹幕失败");
        } finally {
            setDeletingIds(prev => {
                const newSet = new Set(prev);
                newSet.delete(danmakuID);
                return newSet;
            });
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
        <div className="member-danmaku-management">
            <div className="member-page-header">
                <button
                    className="member-back-btn"
                    onClick={navigateMember}
                >
                    ← 返回
                </button>
                <h1 className="member-page-title">管理弹幕</h1>
            </div>

            {danmakus.length === 0 ? (
                <div className="member-empty-state">
                    <div className="member-empty-icon">💬</div>
                    <div className="member-empty-text">暂无弹幕</div>
                </div>
            ) : (
                <div className="member-danmaku-list">
                    {danmakus.map((danmaku) => (
                        <div key={danmaku.danmakuID} className="member-danmaku-item">
                            <div className="member-danmaku-content">
                                <div className="member-danmaku-text">{danmaku.content}</div>
                                <div className="member-danmaku-info">
                                    <span className="member-danmaku-time">
                                        {formatDuration(danmaku.time)}
                                    </span>
                                    <span className="member-danmaku-user">
                                        {danmaku.user.username}
                                    </span>
                                    <span className="member-danmaku-date">
                                        {formatTime(danmaku.createdAt)}
                                    </span>
                                </div>
                            </div>
                            <button
                                className="member-danmaku-delete"
                                onClick={() => handleDeleteDanmaku(danmaku.danmakuID)}
                                disabled={deletingIds.has(danmaku.danmakuID)}
                            >
                                {deletingIds.has(danmaku.danmakuID) ? "删除中..." : "删除"}
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default MemberDanmakuManagement;
