import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useNavigateMember } from "Globals/Navigate";
import { useUserToken } from "Globals/GlobalStore";
import { QueryVideoDanmakuMessage } from "Plugins/DanmakuService/APIs/QueryVideoDanmakuMessage";
import { DeleteDanmakuMessage } from "Plugins/DanmakuService/APIs/DeleteDanmakuMessage";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { formatTime, formatDuration } from "Components/Formatter";
import DefaultAvatar from "Images/DefaultAvatar.jpg";
import { useTopSuccessToast } from "Components/TopSuccessToast/useTopSuccessToast";
import { Danmaku } from "Plugins/DanmakuService/Objects/Danmaku";

interface DanmakuWithUser {
    danmaku: Danmaku;
    user: UserInfo;
}

const MemberDanmakuManagement: React.FC = () => {
    const { videoID } = useParams<{ videoID: string }>();
    const { navigateMember } = useNavigateMember();
    const userToken = useUserToken();
    const [danmakus, setDanmakus] = useState<DanmakuWithUser[]>([]);
    const [loading, setLoading] = useState(true);
    const { ToastComponent, showSuccess } = useTopSuccessToast();

    useEffect(() => {
        if (videoID) {
            loadDanmakus();
        }
        // eslint-disable-next-line
    }, [videoID]);

    const loadDanmakus = async () => {
        if (!videoID) return;
        try {
            setLoading(true);
            const danmakusData = await new Promise<Danmaku[]>((resolve, reject) => {
                new QueryVideoDanmakuMessage(parseInt(videoID)).send(
                    (info: string) => resolve(JSON.parse(info) as Danmaku[]),
                    (error: string) => reject(new Error(error))
                );
            });
            // 获取所有弹幕的用户信息
            const danmakusWithUser: DanmakuWithUser[] = await Promise.all(
                danmakusData.map(async (danmaku: any) => {
                    const user = await new Promise<UserInfo>((resolve, reject) => {
                        new QueryUserInfoMessage(danmaku.userID).send(
                            (info: string) => resolve(JSON.parse(info) as UserInfo),
                            (error: string) => reject(new Error(error))
                        );
                    });
                    return { danmaku, user };
                })
            );
            setDanmakus(danmakusWithUser);
        } catch (error) {
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取弹幕列表失败");
        } finally {
            setLoading(false);
        }
    };

    const handleDeleteDanmaku = async (danmakuID: number) => {
        try {
            await new Promise<void>((resolve, reject) => {
                new DeleteDanmakuMessage(userToken, danmakuID).send(
                    (info: string) => resolve(),
                    (error: string) => reject(new Error(error))
                );
            });
            setDanmakus(prev => prev.filter(item => item.danmaku.danmakuID !== danmakuID));
            showSuccess("弹幕已被删除");
        } catch (error) {
            materialAlertError("删除失败", error instanceof Error ? error.message : "删除弹幕失败");
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
            {ToastComponent}
            <div className="member-page-header">
                <button
                    className="member-back-btn"
                    onClick={navigateMember}
                >
                    ← 返回
                </button>
                <h1 className="member-page-title">管理弹幕</h1>
                <p className="member-page-subtitle">共有 {danmakus.length} 条弹幕</p>
            </div>

            {danmakus.length === 0 ? (
                <div className="member-empty-state">
                    <div className="member-empty-icon">💬</div>
                    <div className="member-empty-text">暂无弹幕</div>
                </div>
            ) : (
                <div className="member-danmaku-list">
                    {danmakus.map(({ danmaku, user }) => (
                        <div key={danmaku.danmakuID} className="member-danmaku-item">
                            <div className="member-danmaku-avatar">
                                <img
                                    src={user?.avatarPath || DefaultAvatar}
                                    alt="头像"
                                    className="member-danmaku-avatar-img"
                                    onError={e => (e.currentTarget.src = DefaultAvatar)}
                                />
                            </div>
                            <div className="member-danmaku-content">
                                <div className="member-danmaku-text">{danmaku.content}</div>
                                <div className="member-danmaku-info">
                                    <span className="member-danmaku-time">{formatDuration(danmaku.timeInVideo)}</span>
                                    <span className="member-danmaku-user">{user ? user.username : '未知用户'}</span>
                                </div>
                            </div>
                            <button
                                className="member-danmaku-delete"
                                onClick={() => handleDeleteDanmaku(danmaku.danmakuID)}
                            >
                                {"删除"}
                            </button>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default MemberDanmakuManagement;
