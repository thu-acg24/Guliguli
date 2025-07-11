// src/Pages/HomePage/HomePage.tsx
import React, { useEffect, useState, useCallback } from "react";
import { useParams, useNavigate, Outlet, useLocation } from "react-router-dom";
import Header from "Components/Header/Header";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import { QueryUserStatMessage } from "Plugins/UserService/APIs/QueryUserStatMessage";
import { UserStat } from "Plugins/UserService/Objects/UserStat";
import { QueryUserVideosMessage } from "Plugins/VideoService/APIs/QueryUserVideosMessage";
import { QueryFollowMessage } from "Plugins/UserService/APIs/QueryFollowMessage";
import { useUserToken, useUserID } from "Globals/GlobalStore";
import UserInfoSection from "./HomePage/UserInfoSection";
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
        navigate(`${homePagePath.replace(":user_id", String(user_id))}/${tab}`);
    }, [navigate, navigateHome]);

    return { navigateHome, navigateHomeTab };
}

const HomePage: React.FC = () => {
    const { user_id } = useParams<{ user_id: string }>();
    const { navigateHomeTab } = useNavigateHome();
    const location = useLocation();
    const userToken = useUserToken();
    const { userID: currentUserID } = useUserID();
    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
    const [userStat, setUserStat] = useState<UserStat | null>(null);
    const [videoCount, setVideoCount] = useState<number>(0);
    const [loading, setLoading] = useState(true);
    const [isCurrentUser, setIsCurrentUser] = useState(false);
    const [isFollowing, setIsFollowing] = useState(false);

    // 获取用户信息
    const fetchUserInfo = async () => {
        try {
            const userIdNum = parseInt(user_id);
            setUserInfo(await new Promise<UserInfo>((resolve, reject) => {
                new QueryUserInfoMessage(userIdNum).send(
                    (info: string) => resolve(JSON.parse(info) as UserInfo),
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

    // 通过路由获取当前激活tab
    const activeTab = getTab(location.pathname);

    // 侧边栏点击跳转
    const handleTabClick = (tab: HomePageTab) => {
        navigateHomeTab(user_id!, tab);
    };

    return (
        <div className="home-home-page">
            <Header />
            <UserInfoSection
                user_id={user_id!}
                currentUserID={currentUserID}
                userToken={userToken}
                isCurrentUser={isCurrentUser}
                fetchUserStat={fetchUserStat}
                userInfo={userInfo}
                setUserInfo={setUserInfo}
                isFollowing={isFollowing}
                setIsFollowing={setIsFollowing}
            />

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