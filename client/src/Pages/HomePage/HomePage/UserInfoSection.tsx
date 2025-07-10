// src/Pages/HomePage/HomePage/UserInfoSection.tsx
import React, { useState } from "react";
import back from "Images/back.jpg";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { ModifyUserInfoMessage } from "Plugins/UserService/APIs/ModifyUserInfoMessage";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { ChangeFollowStatusMessage } from "Plugins/UserService/APIs/ChangeFollowStatusMessage";
import { MessagePageTab, useNavigateMessage } from "Globals/Navigate";


const UserInfoSection: React.FC<{
    user_id: string, currentUserID: number | null, userToken: string | null,
    isCurrentUser: boolean, fetchUserStat: () => void,
    userInfo: UserInfo | null, setUserInfo: (info: UserInfo) => void,
    isFollowing: boolean, setIsFollowing: (following: boolean) => void
}> = ({ user_id, currentUserID, userToken, isCurrentUser, fetchUserStat, userInfo, setUserInfo, isFollowing, setIsFollowing, }) => {
    const { navigateMessageTab } = useNavigateMessage();
    const [isEditingBio, setIsEditingBio] = useState(false);
    const [tempBio, setTempBio] = useState("");


    // 开始编辑个性签名
    const startEditingBio = () => {
        setTempBio(userInfo?.bio || "");
        setIsEditingBio(true);
    };

    // 保存个性签名
    const saveBio = async () => {
        try {
            // 第一步：创建新的用户信息对象
            const newUserInfo = new UserInfo(
                userInfo.userID,
                userInfo.username,
                userInfo.avatarPath,
                userInfo.isBanned,
                tempBio
            );

            // 第二步：发送更新请求
            await new Promise<void>((resolve, reject) => {
                new ModifyUserInfoMessage(userToken, newUserInfo).send(
                    (info: string) => {
                        // 请求成功后更新本地状态
                        setUserInfo(newUserInfo);
                        resolve();
                    },
                    (error: string) => {
                        reject(new Error(error));
                    }
                );
            });
        } catch (error) {
            console.error("保存个性签名失败", error);
            materialAlertError("保存失败", "无法保存个性签名，请稍后再试。");
            return;
        }
        setIsEditingBio(false);
    };

    // 取消编辑个性签名
    const cancelEditingBio = () => {
        setIsEditingBio(false);
        setTempBio("");
    };

    // 处理键盘事件
    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            saveBio();
        } else if (e.key === 'Escape') {
            cancelEditingBio();
        }
    };

    // 处理关注/取消关注
    const handleFollowToggle = async () => {
        if (!user_id || !currentUserID || !userToken) {
            console.warn("用户未登录，无法执行关注操作");
            return;
        }

        const userIdNum = parseInt(user_id);
        if (currentUserID === userIdNum) {
            console.warn("不能关注自己");
            return;
        }

        try {
            const newFollowingStatus = !isFollowing;

            // 发送关注状态更改请求
            await new Promise<void>((resolve, reject) => {
                new ChangeFollowStatusMessage(
                    userToken,
                    userIdNum,
                    newFollowingStatus
                ).send(
                    (info: string) => {
                        console.log("关注状态更改成功", info);
                        resolve();
                    },
                    (error: any) => {
                        console.error("关注状态更改失败", error);
                        // 如果失败，恢复原状态
                        setIsFollowing(!newFollowingStatus);
                        reject(error);
                    }
                );
            });

            setIsFollowing(newFollowingStatus);
            fetchUserStat();
        } catch (error) {
            console.error("关注操作失败", error);
            materialAlertError("操作失败", "关注操作失败，请稍后再试");
        }
    };

    // 处理私信按钮点击
    const handleMessageClick = () => {
        console.log(`点击私信按钮，目标用户ID: ${user_id}`);
        navigateMessageTab(MessagePageTab.whisper, user_id);
    };

    return (
        <div
            className="home-user-info-section"
            style={{
                background: `url(${back}) center/cover no-repeat`,
                position: "relative",
                color: "white",
                padding: "30px 40px",
            }}
        >
            <div className="home-user-left">
                <div className="home-info-user-avatar">
                    <img src={userInfo.avatarPath} alt="用户头像" />
                </div>
                <div className="home-user-basic-info">
                    <div style={{ display: 'flex', alignItems: 'flex-end', gap: '10px' }}>
                        <div className="home-profile-user-name">{userInfo.username}</div>
                        <div className="home-user-id" style={{ fontSize: '15px', color: 'white' }}>ID: {user_id}</div>
                    </div>
                    <div className="home-user-signature">
                        {isEditingBio ? (
                            <input
                                type="text"
                                value={tempBio}
                                onChange={(e) => setTempBio(e.target.value)}
                                onKeyDown={handleKeyDown}
                                onBlur={saveBio}
                                className="home-bio-input"
                                placeholder="编辑个性签名"
                                autoFocus
                            />
                        ) : (
                            <div
                                className={`home-bio-text ${isCurrentUser ? 'editable' : ''}`}
                                onClick={isCurrentUser ? startEditingBio : undefined}
                            >
                                {userInfo.bio}
                            </div>
                        )}
                    </div>
                </div>
            </div>
            {/* 关注按钮 - 只有当前用户已登录且不是自己时才显示 */}
            {currentUserID && !isCurrentUser && (
                <div className="home-user-actions">
                    <button
                        className="home-message-btn"
                        onClick={handleMessageClick}
                    >
                        私信
                    </button>
                    <button
                        className={`home-follow-btn ${isFollowing ? 'following' : ''}`}
                        onClick={handleFollowToggle}
                    >
                        {isFollowing ? '已关注' : '关注'}
                    </button>
                </div>
            )}
        </div>
    )
}
export default UserInfoSection;