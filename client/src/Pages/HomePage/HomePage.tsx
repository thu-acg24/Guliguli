// src/Pages/HomePage/HomePage.tsx
import React, { useEffect, useState } from "react";
import { useParams, useNavigate, Outlet, useLocation } from "react-router-dom";
import Header from "Components/Header/Header";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { ModifyUserInfoMessage } from "Plugins/UserService/APIs/ModifyUserInfoMessage";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import { QueryUserStatMessage } from "Plugins/UserService/APIs/QueryUserStatMessage";
import { UserStat } from "Plugins/UserService/Objects/UserStat";
import { QueryUserVideosMessage } from "Plugins/VideoService/APIs/QueryUserVideosMessage";
import { useUserToken, useUserID } from "Globals/GlobalStore";
import "./HomePage.css";

export const homePagePath = "/home/:user_id";

// 定义页面标签常量
const TAB_VIDEOS = "videos";
const TAB_FOLLOWING = "following";
const TAB_FOLLOWERS = "followers";
const TAB_FAVORITES = "favorites";
const TAB_HISTORY = "history";
const TAB_SETTINGS = "settings";

const HomePage: React.FC = () => {
    const { user_id } = useParams<{ user_id: string }>();
    const navigate = useNavigate();
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

    // 获取用户信息
    const fetchUserInfo = async () => {
        if (!user_id) return;

        try {
            setLoading(true);
            const userIdNum = parseInt(user_id);
            new QueryUserInfoMessage(userIdNum).send(
                (info: string) => {
                    const data = JSON.parse(info);
                    if (data.isBanned) {
                        setUserInfo(null);
                    } else {
                        setUserInfo(data);
                    }
                    setLoading(false);
                },
                (error: string) => {
                    materialAlertError("加载失败", error);
                    setUserInfo(null);
                    setLoading(false);
                }
            );
        } catch (error) {
            materialAlertError("加载失败", error.message);
            setUserInfo(null);
            setLoading(false);
        }
    };

    // 获取用户统计信息
    const fetchUserStat = async () => {
        if (!user_id) return;

        try {
            setLoading(true);
            const userIdNum = parseInt(user_id);
            new QueryUserStatMessage(userIdNum).send(
                (info: string) => {
                    const data = JSON.parse(info);
                    setUserStat(data);
                    setLoading(false);
                },
                (error: string) => {
                    console.error("获取用户统计信息失败", error);
                    setUserStat(null);
                    setLoading(false);
                }
            );
        } catch (error) {
            console.error("获取用户统计信息失败", error);
            setUserStat(null);
            setLoading(false);
        }
    };

    // 获取用户视频数
    const fetchUserVideoCount = async () => {
        if (!user_id) return;

        try {
            setLoading(true);
            const userIdNum = parseInt(user_id);
            new QueryUserVideosMessage(null, userIdNum).send(
                (info: string) => {
                    const data = JSON.parse(info);
                    setVideoCount(Array.isArray(data) ? data.length : 0);
                    setLoading(false);
                },
                (error: string) => {
                    console.error("获取用户视频数失败", error);
                    setVideoCount(0);
                    setLoading(false);
                }
            );
        } catch (error) {
            console.error("获取用户视频数失败", error);
            setVideoCount(0);
            setLoading(false);
        }
    };

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
            new ModifyUserInfoMessage(userToken, newUserInfo).send(
                (info: string) => {
                    // 请求成功后更新本地状态
                    setUserInfo(newUserInfo);
                },
                (error: string) => {
                    throw new Error(error);
                }
            );
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

    useEffect(() => {
        console.log('检查是否是当前用户')
        fetchUserInfo();
        fetchUserStat();
        fetchUserVideoCount();
        // 检查是否是当前用户
        const checkIfCurrentUser = () => {
            setIsCurrentUser(!!currentUserID && currentUserID === parseInt(user_id || ""));
        };

        checkIfCurrentUser();
        console.log('isCurrentUser:', isCurrentUser);
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

    // 通过路由获取当前激活tab
    const getActiveTab = () => {
        const pathParts = location.pathname.split("/");
        // /home/:user_id 或 /home/:user_id/xxx
        if (pathParts.length < 4) return TAB_VIDEOS;
        return pathParts[3] || TAB_VIDEOS;
    };
    const activeTab = getActiveTab();

    // 侧边栏点击跳转
    const handleTabClick = (tab: string) => {
        const basePath = homePagePath.replace(":user_id", user_id!);
        if (tab === TAB_VIDEOS) {
            navigate(basePath);
        } else {
            navigate(`${basePath}/${tab}`);
        }
    };

    return (
        <div className="home-home-page">
            <Header />
            {/* 用户信息区域 */}
            <div className="home-user-info-section">
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
            </div>

            {/* 内容区域 */}
            <div className="home-content-section">
                {/* 侧边栏导航 */}
                <div className="home-sidebar">
                    <div
                        className={`home-sidebar-item ${activeTab === TAB_VIDEOS ? "active" : ""}`}
                        onClick={() => handleTabClick(TAB_VIDEOS)}
                    >
                        <span>视频</span>
                        <span className="home-sidebar-count">{videoCount}</span>
                    </div>
                    <div
                        className={`home-sidebar-item ${activeTab === TAB_FOLLOWING ? "active" : ""}`}
                        onClick={() => handleTabClick(TAB_FOLLOWING)}
                    >
                        <span>关注</span>
                        <span className="home-sidebar-count">{userStat?.followingCount || 0}</span>
                    </div>
                    <div
                        className={`home-sidebar-item ${activeTab === TAB_FOLLOWERS ? "active" : ""}`}
                        onClick={() => handleTabClick(TAB_FOLLOWERS)}
                    >
                        <span>粉丝</span>
                        <span className="home-sidebar-count">{userStat?.followerCount || 0}</span>
                    </div>
                    {/* 仅当前用户可见的选项 */}
                    {isCurrentUser && (
                        <>
                            <div
                                className={`home-sidebar-item ${activeTab === TAB_FAVORITES ? "active" : ""}`}
                                onClick={() => handleTabClick(TAB_FAVORITES)}
                            >
                                收藏
                            </div>
                            <div
                                className={`home-sidebar-item ${activeTab === TAB_HISTORY ? "active" : ""}`}
                                onClick={() => handleTabClick(TAB_HISTORY)}
                            >
                                历史记录
                            </div>
                            <div
                                className={`home-sidebar-item ${activeTab === TAB_SETTINGS ? "active" : ""}`}
                                onClick={() => handleTabClick(TAB_SETTINGS)}
                            >
                                设置
                            </div>
                        </>
                    )}
                </div>

                {/* 主内容区域 */}
                <div className="home-main-content">
                    <div className="home-tab-title">
                        {activeTab === TAB_VIDEOS && "发布的视频"}
                        {activeTab === TAB_FOLLOWING && "关注列表"}
                        {activeTab === TAB_FOLLOWERS && "粉丝列表"}
                        {activeTab === TAB_FAVORITES && "收藏的视频"}
                        {activeTab === TAB_HISTORY && "观看历史"}
                        {activeTab === TAB_SETTINGS && "个人设置"}
                    </div>

                    {/* 检查是否有权限访问私人内容 */}
                    {!isCurrentUser && (activeTab === TAB_FAVORITES || activeTab === TAB_HISTORY || activeTab === TAB_SETTINGS) ? (
                        <div className="home-error-message">您没有权限访问此内容</div>
                    ) : (
                        <Outlet context={{ userID: userInfo.userID, userInfo, isCurrentUser, refreshUserInfo: fetchUserInfo }} />
                    )}
                </div>
            </div>
        </div>
    );
};

export default HomePage;