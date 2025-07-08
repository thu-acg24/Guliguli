// src/Pages/HomePage/HomePage.tsx
import React, { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate, Outlet, useLocation } from "react-router-dom";
import Header from "Components/Header/Header";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { ModifyUserInfoMessage } from "Plugins/UserService/APIs/ModifyUserInfoMessage";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import { QueryUserStatMessage } from "Plugins/UserService/APIs/QueryUserStatMessage";
import { UserStat } from "Plugins/UserService/Objects/UserStat";
import { QueryUserVideosMessage } from "Plugins/VideoService/APIs/QueryUserVideosMessage";
import { QueryFollowMessage } from "Plugins/UserService/APIs/QueryFollowMessage";
import { ChangeFollowStatusMessage } from "Plugins/UserService/APIs/ChangeFollowStatusMessage";
import { useUserToken, useUserID } from "Globals/GlobalStore";
import { WhisperTabpath } from "Pages/MessagePage/WhisperTab"
import back from "Images/back.jpg";
import "./HomePage.css";

export const homePagePath = "/home/:user_id";

// 定义页面标签枚举
export enum HomePageTab {
    videos = "videos",
    following = "following",
    followers = "followers",
    favorites = "favorites",
    history = "history",
    settings = "settings",
}

const getTab = (path: string): HomePageTab => {
    const parts = path.split("/");
    if (parts.length < 4) return HomePageTab.videos; // 默认返回视频标签
    const tab = parts[3];
    return Object.values(HomePageTab).includes(tab as HomePageTab) ? (tab as HomePageTab) : HomePageTab.videos;
}

export function useNavigateHome() {
    const navigate = useNavigate();
    const navigateHome = useCallback((user_id: string | number) => {
        navigate(homePagePath.replace(":user_id", String(user_id)));
    }, [navigate]);

    const navigateHomeTab = useCallback((user_id: string | number, tab: HomePageTab) => {
        if (tab === HomePageTab.videos) {
            navigateHome(user_id);
        } else {
            navigate(`${homePagePath.replace(":user_id", String(user_id))}/${tab}`);
        }
    }, [navigate, navigateHome]);

    return { navigateHome, navigateHomeTab };
}

const HomePage: React.FC = () => {
    const { user_id } = useParams<{ user_id: string }>();
    const navigate = useNavigate();
    const { navigateHomeTab } = useNavigateHome();
    const location = useLocation();
    const userToken = useUserToken();
    const { userID: currentUserID } = useUserID();
    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
    const [userStat, setUserStat] = useState<UserStat | null>(null);
    const [videoCount, setVideoCount] = useState<number>(0);
    const [loading, setLoading] = useState(true);
    const [isCurrentUser, setIsCurrentUser] = useState(false);
    const [isEditingBio, setIsEditingBio] = useState(false);
    const [tempBio, setTempBio] = useState("");
    const [isFollowing, setIsFollowing] = useState(false);

    // 获取用户信息
    const fetchUserInfo = async () => {
        try {
            const userIdNum = parseInt(user_id);
            setUserInfo(await new Promise<UserInfo>((resolve, reject) => {
                new QueryUserInfoMessage(userIdNum).send(
                    (info: string) => {
                        const data = JSON.parse(info);
                        if (data.isBanned) {
                            resolve(null);
                        } else {
                            resolve(data);
                        }
                    },
                    (error: string) => {
                        reject(error);
                    }
                );
            }));
        } catch (error) {
            materialAlertError("加载失败", error.message);
            setUserInfo(null);
        }
    };

    // 获取用户统计信息
    const fetchUserStat = async () => {
        try {
            const userIdNum = parseInt(user_id);
            const userStat = await new Promise<UserStat>((resolve, reject) => {
                new QueryUserStatMessage(userIdNum).send(
                    (info: string) => { resolve(JSON.parse(info) as UserStat); },
                    (error: string) => { reject(error); }
                );
            });
            setUserStat(userStat);
        } catch (error) {
            console.error("获取用户统计信息失败", error);
            setUserStat(null);
        }
    };

    // 获取用户视频数
    const fetchUserVideoCount = async () => {
        try {
            const userIdNum = parseInt(user_id);
            const videoData = await new Promise<any>((resolve, reject) => {
                new QueryUserVideosMessage(null, userIdNum).send(
                    (info: string) => { resolve(JSON.parse(info)); },
                    (error: string) => { reject(error); }
                );
            });
            setVideoCount(Array.isArray(videoData) ? videoData.length : 0);
        } catch (error) {
            console.error("获取用户视频数失败", error);
            setVideoCount(0);
        }
    };

    // 获取关注状态
    const fetchFollowStatus = async () => {
        try {
            const userIdNum = parseInt(user_id);
            const followResult = await new Promise<boolean>((resolve, reject) => {
                new QueryFollowMessage(currentUserID, userIdNum).send(
                    (info: string) => { resolve(JSON.parse(info)); },
                    (error: string) => { reject(error); }
                );
            });
            setIsFollowing(followResult);
        } catch (error) {
            console.error("获取关注状态失败", error);
            setIsFollowing(false);
        }
    };

    useEffect(() => {
        setLoading(true);
        const loadData = async () => {
            try {
                await Promise.all([
                    fetchUserInfo(),
                    fetchUserStat(),
                    fetchUserVideoCount(),
                    fetchFollowStatus()
                ]);
                setIsCurrentUser(!!currentUserID && currentUserID === parseInt(user_id || ""));
                console.log('isCurrentUser:', isCurrentUser);
            } catch (error) {
                console.error('加载数据失败:', error);
            } finally {
                setLoading(false);
            }
        };
        loadData();
    }, [user_id, currentUserID]);

    if (loading) {
        return (
            <div className="home-home-page">
                <Header />
                <div className="home-loading">加载中...</div>
            </div>
        );
    }

    if (!userInfo) {
        return (
            <div className="home-home-page">
                <Header />
                <div className="home-error-message">获取用户信息错误</div>
            </div>
        );
    }

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
        navigate(`${WhisperTabpath}/${user_id}`);
    };

    // 通过路由获取当前激活tab
    const activeTab = getTab(location.pathname);

    // 侧边栏点击跳转
    const handleTabClick = (tab: HomePageTab) => {
        navigateHomeTab(user_id!, tab);
    };

    return (
        <div className="home-home-page">
            <Header />
            {/* 用户信息区域 */}
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
                        <div className="home-profile-user-name">{userInfo.username}</div>
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

            {/* 内容区域 */}
            <div className="home-content-section">
                {/* 侧边栏导航 */}
                <div className="home-sidebar">
                    <div
                        className={`home-sidebar-item ${activeTab === HomePageTab.videos ? "active" : ""}`}
                        onClick={() => handleTabClick(HomePageTab.videos)}
                    >
                        <span>视频</span>
                        <span className="home-sidebar-count">{videoCount}</span>
                    </div>
                    <div
                        className={`home-sidebar-item ${activeTab === HomePageTab.following ? "active" : ""}`}
                        onClick={() => handleTabClick(HomePageTab.following)}
                    >
                        <span>关注</span>
                        <span className="home-sidebar-count">{userStat?.followingCount || 0}</span>
                    </div>
                    <div
                        className={`home-sidebar-item ${activeTab === HomePageTab.followers ? "active" : ""}`}
                        onClick={() => handleTabClick(HomePageTab.followers)}
                    >
                        <span>粉丝</span>
                        <span className="home-sidebar-count">{userStat?.followerCount || 0}</span>
                    </div>
                    {/* 仅当前用户可见的选项 */}
                    {isCurrentUser && (
                        <>
                            <div
                                className={`home-sidebar-item ${activeTab === HomePageTab.favorites ? "active" : ""}`}
                                onClick={() => handleTabClick(HomePageTab.favorites)}
                            >
                                收藏
                            </div>
                            <div
                                className={`home-sidebar-item ${activeTab === HomePageTab.history ? "active" : ""}`}
                                onClick={() => handleTabClick(HomePageTab.history)}
                            >
                                历史记录
                            </div>
                            <div
                                className={`home-sidebar-item ${activeTab === HomePageTab.settings ? "active" : ""}`}
                                onClick={() => handleTabClick(HomePageTab.settings)}
                            >
                                设置
                            </div>
                        </>
                    )}
                </div>

                {/* 主内容区域 */}
                <div className="home-main-content">
                    <div className="home-tab-title">
                        {activeTab === HomePageTab.videos && "发布的视频"}
                        {activeTab === HomePageTab.following && "关注列表"}
                        {activeTab === HomePageTab.followers && "粉丝列表"}
                        {activeTab === HomePageTab.favorites && "收藏的视频"}
                        {activeTab === HomePageTab.history && "观看历史"}
                        {activeTab === HomePageTab.settings && "个人设置"}
                    </div>

                    {/* 检查是否有权限访问私人内容 */}
                    {!isCurrentUser && (activeTab === HomePageTab.favorites || activeTab === HomePageTab.history || activeTab === HomePageTab.settings) ? (
                        <div className="home-error-message">您没有权限访问此内容</div>
                    ) : (
                        <Outlet context={{ userID: userInfo.userID, userInfo, isCurrentUser, refreshUserInfo: fetchUserInfo, refreshUserStat: fetchUserStat, refreshUserVideoCount: fetchUserVideoCount, isFollowing: isFollowing }} />
                    )}
                </div>
            </div>
        </div>
    );
};

export default HomePage;