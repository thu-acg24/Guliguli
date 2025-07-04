// src/Pages/HomePage/HomePage.tsx
import React, { useEffect, useState } from "react";
import { useParams, useNavigate, Outlet, useLocation } from "react-router-dom";
import Header from "Components/Header/Header";
import { useUserToken } from "Globals/GlobalStore";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import { GetUIDByTokenMessage } from "Plugins/UserService/APIs/GetUIDByTokenMessage";
import { QueryUserStatMessage } from "Plugins/UserService/APIs/QueryUserStatMessage";
import { UserStat } from "Plugins/UserService/Objects/UserStat";
import { QueryUserVideosMessage } from "Plugins/VideoService/APIs/QueryUserVideosMessage";
import "./HomePage.css";

export const homePagePath = "/home/:user_id";

const HomePage: React.FC = () => {
    const { user_id } = useParams<{ user_id: string }>();
    const navigate = useNavigate();
    const userToken = useUserToken();
    const location = useLocation();
    const [currentUserID, setCurrentUserID] = useState<number | null>(null);
    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
    const [userStat, setUserStat] = useState<UserStat | null>(null);
    const [videoCount, setVideoCount] = useState<number>(0);
    const [loading, setLoading] = useState(true);
    const [isCurrentUser, setIsCurrentUser] = useState(false);

    // 获取当前登录用户ID
    const getUserIDByToken = async () => {
        if (!userToken) return null;
        try {
            return new Promise<number>((resolve) => {
                new GetUIDByTokenMessage(userToken).send(
                    (info: string) => resolve(JSON.parse(info)),
                    (error: string) => {
                        console.error("获取当前用户ID失败", error);
                        resolve(0);
                    }
                );
            });
        } catch (error) {
            console.error("获取当前用户ID失败", error);
            return null;
        }
    };

    // 获取用户信息
    const fetchUserInfo = async () => {
        if (!user_id) return;

        // mock 开关，true=使用mock，false=调用真实API
        const useMock = false;
        if (useMock) {
            setLoading(true);
            setTimeout(() => {
                if (user_id === '404') {
                    setUserInfo(null);
                } else {
                    setUserInfo(new UserInfo(
                        Number(user_id),
                        `测试用户${user_id}`,
                        '',
                        false
                    ));
                }
                setLoading(false);
            }, 500);
            return;
        }

        try {
            setLoading(true);
            const userIdNum = parseInt(user_id);
            new QueryUserInfoMessage(userIdNum).send(
                (info: string) => {
                    const data = JSON.parse(info);
                    if (!data || data.isBanned) {
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

    useEffect(() => {
        fetchUserInfo();
        fetchUserStat();
        fetchUserVideoCount();
        // 检查是否是当前用户
        const checkIfCurrentUser = async () => {
            const currentID = await getUserIDByToken();
            setCurrentUserID(currentID);
            setIsCurrentUser(!!currentID && currentID === parseInt(user_id || ""));
        };

        checkIfCurrentUser();
    }, [user_id, userToken]);

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
        if (pathParts.length < 4) return "videos";
        return pathParts[3] || "videos";
    };
    const activeTab = getActiveTab();

    // 侧边栏点击跳转
    const handleTabClick = (tab: string) => {
        if (tab === "videos") {
            navigate(`/home/${user_id}`);
        } else {
            navigate(`/home/${user_id}/${tab}`);
        }
    };

    return (
        <div className="home-home-page">
            <Header />
            {/* 用户信息区域 */}
            <div className="home-user-info-section">
                <div className="home-user-left">
                    <div className="home-user-avatar">
                        <img src={userInfo.avatarPath} alt="用户头像" />
                    </div>
                    <div className="home-user-basic-info">
                        <div className="home-user-name">{userInfo.username}</div>
                    </div>
                </div>
            </div>

            {/* 内容区域 */}
            <div className="home-content-section">
                {/* 侧边栏导航 */}
                <div className="home-sidebar">
                    <div
                        className={`home-sidebar-item ${activeTab === "videos" ? "active" : ""}`}
                        onClick={() => handleTabClick("videos")}
                    >
                        <span>视频</span>
                        <span className="home-sidebar-count">{videoCount}</span>
                    </div>
                    <div
                        className={`home-sidebar-item ${activeTab === "following" ? "active" : ""}`}
                        onClick={() => handleTabClick("following")}
                    >
                        <span>关注</span>
                        <span className="home-sidebar-count">{userStat?.followingCount || 0}</span>
                    </div>
                    <div
                        className={`home-sidebar-item ${activeTab === "followers" ? "active" : ""}`}
                        onClick={() => handleTabClick("followers")}
                    >
                        <span>粉丝</span>
                        <span className="home-sidebar-count">{userStat?.followerCount || 0}</span>
                    </div>
                    {/* 仅当前用户可见的选项 */}
                    {isCurrentUser && (
                        <>
                            <div
                                className={`home-sidebar-item ${activeTab === "favorites" ? "active" : ""}`}
                                onClick={() => handleTabClick("favorites")}
                            >
                                收藏
                            </div>
                            <div
                                className={`home-sidebar-item ${activeTab === "history" ? "active" : ""}`}
                                onClick={() => handleTabClick("history")}
                            >
                                历史记录
                            </div>
                            <div
                                className={`home-sidebar-item ${activeTab === "settings" ? "active" : ""}`}
                                onClick={() => handleTabClick("settings")}
                            >
                                设置
                            </div>
                        </>
                    )}
                </div>

                {/* 主内容区域 */}
                <div className="home-main-content">
                    <div className="home-tab-title">
                        {activeTab === "videos" && "发布的视频"}
                        {activeTab === "following" && "关注列表"}
                        {activeTab === "followers" && "粉丝列表"}
                        {activeTab === "favorites" && "收藏的视频"}
                        {activeTab === "history" && "观看历史"}
                        {activeTab === "settings" && "个人设置"}
                    </div>

                    <Outlet context={{ userID: userInfo.userID, userInfo, isCurrentUser }} />
                </div>
            </div>
        </div>
    );
};

export default HomePage;