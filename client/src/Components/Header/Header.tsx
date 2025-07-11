import React, { useEffect, useState } from "react";
import { useNavigateAudit, useNavigateAdmin, useNavigateMain, useNavigateHome, HomePageTab, useNavigateMember, useNavigateMessage, useNavigateSearch } from "Globals/Navigate";
import iconSrc from "Images/LOGO.png";
import LoginModal from "Components/LoginModal/LoginModal";
import { useUserToken, setUserToken, useUserInfo, useUserStat, useUserID } from "Globals/GlobalStore";
import { useUserRole } from "Hooks/useUserRole";
import { LogoutMessage } from "Plugins/UserService/APIs/LogoutMessage";
import { PersonCenterIcon, LogoutIcon } from "Images/Icons";
import DEFAULT_AVATAR from "Images/DefaultAvatar.jpg";
import { SendIcon, HollowFavoriteIcon, HistoryIcon, UploadIcon, SearchIcon, NewsIcon } from "Images/Icons";
import { QueryFollowingVideosMessage } from "Plugins/VideoService/APIs/QueryFollowingVideosMessage";
import { Video } from "Plugins/VideoService/Objects/Video";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import { QueryUserVideosMessage } from "Plugins/VideoService/APIs/QueryUserVideosMessage";
import NewsPanel from "./NewsPanel";
import "./Header.css";
import { set } from "lodash";

export interface VideoWithUploader {
    video: Video;
    uploaderInfo: UserInfo;
}

const Header: React.FC<{ usetransparent?: boolean, transparent?: boolean, hideSearch?: boolean }> = ({ usetransparent = false, transparent = false, hideSearch = false }) => {
    const { navigateAudit } = useNavigateAudit();
    const { navigateAdmin } = useNavigateAdmin();
    const { navigateMain } = useNavigateMain();
    const { navigateHome, navigateHomeTab } = useNavigateHome();
    const { navigateMember } = useNavigateMember();
    const { navigateMessage } = useNavigateMessage();
    const { navigateSearch } = useNavigateSearch();
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [searchKeyword, setSearchKeyword] = useState("");
    const [showUserPanel, setShowUserPanel] = useState(false);
    const userToken = useUserToken();
    const { userInfo } = useUserInfo();
    const { userStat } = useUserStat();
    const { userID } = useUserID();
    const { isAuditor, isAdmin } = useUserRole();
    const [videoResults, setVideoResults] = useState<VideoWithUploader[]>([]);
    const [showNewsPanel, setShowNewsPanel] = useState(false);
    const [userVideos, setUserVideos] = useState<number>(0);

    useEffect(() => {
        if (userToken && userID) {
            new QueryUserVideosMessage(userToken, userID).send(
                (info: string) => {
                    const videos = JSON.parse(info) as Video[];
                    setUserVideos(videos.length);
                },
                (error: string) => {
                    console.error("查询用户视频失败:", error);
                }
            );
        }
    }, [userToken, userID]);

    // 跳转函数
    const handleAvatarClick = async () => {
        navigateHome(userID);
        setShowUserPanel(false);
    }
    const handleAuditClick = async () => {
        navigateAudit();
        setShowUserPanel(false);
    }
    const handleAdminClick = async () => {
        navigateAdmin();
        setShowUserPanel(false);
    }

    // 处理头像鼠标悬浮
    const handleAvatarMouseEnter = async () => {
        setShowUserPanel(true);
    };

    const handleAvatarMouseLeave = () => {
        setShowUserPanel(false);
    };

    const handleMsgClick = async () => {
        navigateMessage();
    };
    const handleFavClick = async () => {
        navigateHomeTab(userID, HomePageTab.favorites);
    };
    const handleHistoryClick = async () => {
        navigateHomeTab(userID, HomePageTab.history);
    };
    const handleUploadClick = async () => {
        navigateMember();
    };
    const handleFollowingClick = async () => {
        navigateHomeTab(userID, HomePageTab.following);
        setShowUserPanel(false);
    };
    const handleFollowerClick = async () => {
        navigateHomeTab(userID, HomePageTab.followers);
        setShowUserPanel(false);
    };
    const handleUserVideoClick = async () => {
        navigateHomeTab(userID, HomePageTab.videos);
        setShowUserPanel(false);
    };

    // 处理登出
    const handleLogout = async () => {
        if (!userToken) return;
        try {
            new LogoutMessage(userToken).send(
                (info: string) => { },
                (e: string) => { console.error(e) }
            );
        } catch (e) {
            console.error(e);
        }
        setUserToken("");
        setShowUserPanel(false);
    };

    const performSearch = () => {
        navigateSearch(searchKeyword);
        setSearchKeyword("");
    };

    const handleNewsMouseEnter = async () => {
        try {
            const videos = await new Promise<Video[]>((resolve, reject) => {
                new QueryFollowingVideosMessage(userToken, 10, 99999999999999, 9999).send(
                    (info: string) => resolve(JSON.parse(info) as Video[]),
                    (error: string) => reject(new Error(error))
                );
            });
            const videosWithUploader = await Promise.all(
                videos.map(async (video) => {
                    const uploaderInfo = await new Promise<UserInfo>((resolve, reject) => {
                        new QueryUserInfoMessage(video.uploaderID).send(
                            (info: string) => resolve(JSON.parse(info) as UserInfo),
                            (err: string) => reject(new Error(err))
                        );
                    });
                    return { video, uploaderInfo };
                })
            );
            setVideoResults(videosWithUploader);
        } catch (error) {
            console.error("获取动态失败:", error);
            setVideoResults([]);
        } finally {
            setShowNewsPanel(true);
        }
    }

    const handleNewsMouseLeave = () => {
        setShowNewsPanel(false);
    }

    return (
        <header className={`header-header ${usetransparent ? 'usetransparent' : ''} ${transparent ? 'transparent' : ''}`}>
            <div className="header-logo" onClick={() => {
                navigateMain();
                window.scrollTo({ top: 0, behavior: "smooth" });
            }}>
                <img src={iconSrc} alt="GULIGULI" className="header-logo-icon" />
            </div>
            {!hideSearch &&
                <div className="header-search-container">
                    <div className="header-search-box">
                        <input
                            type="text"
                            className="header-search-input"
                            placeholder="搜索视频、UP主"
                            value={searchKeyword}
                            onChange={(e) => setSearchKeyword(e.target.value)}
                            onKeyPress={(e) => e.key === 'Enter' && performSearch()}
                        />
                        <button className="header-search-btn" onClick={performSearch}>
                            <SearchIcon className="header-search-icon" />
                        </button>
                    </div>
                </div>
            }
            {userInfo ? (
                <div className="header-header-actions">
                    <div
                        className="header-user-avatar-container"
                        onMouseEnter={handleAvatarMouseEnter}
                        onMouseLeave={handleAvatarMouseLeave}
                    >
                        <div className="header-user-avatar" onClick={handleAvatarClick}>
                            <img
                                src={userInfo?.avatarPath || DEFAULT_AVATAR}
                                alt="用户头像"
                            />
                        </div>
                        {showUserPanel && (
                            <div className="header-user-panel-popover">
                                {/* 新增放大头像容器 */}
                                <div className="header-zoomed-avatar" onClick={handleAvatarClick}>
                                    <img
                                        src={userInfo?.avatarPath || DEFAULT_AVATAR}
                                        alt="用户头像"
                                    />
                                </div>
                                <div className="header-user-panel-content">
                                    {/* 移除原大头像区域，昵称居中 */}
                                    <div className="header-user-nickname" onClick={handleAvatarClick}>
                                        {userInfo?.username || "用户"}
                                    </div>

                                    <div className="header-user-stats">
                                        <div className="header-stat-item" onClick={handleFollowingClick}>
                                            <div className="header-stat-number">{userStat?.followingCount || 0}</div>
                                            <div className="header-stat-label">关注</div>
                                        </div>
                                        <div className="header-stat-item" onClick={handleFollowerClick}>
                                            <div className="header-stat-number">{userStat?.followerCount || 0}</div>
                                            <div className="header-stat-label">粉丝</div>
                                        </div>
                                        <div className="header-stat-item" onClick={handleUserVideoClick}>
                                            <div className="header-stat-number">{userVideos}</div>
                                            <div className="header-stat-label">视频</div>
                                        </div>
                                    </div>

                                    <div className="header-panel-links">
                                        <div className="header-panel-link-item" onClick={handleAvatarClick}>
                                            <PersonCenterIcon />
                                            <span>个人中心</span>
                                        </div>
                                    </div>

                                    {isAuditor &&
                                        <div className="header-panel-links">
                                            <div className="header-panel-link-item" onClick={handleAuditClick}>
                                                <PersonCenterIcon />
                                                <span>审核中心</span>
                                            </div>
                                        </div>
                                    }

                                    {isAdmin &&
                                        <div className="header-panel-links">
                                            <div className="header-panel-link-item" onClick={handleAdminClick}>
                                                <PersonCenterIcon />
                                                <span>管理中心</span>
                                            </div>
                                        </div>
                                    }

                                    <div className="header-panel-divider"></div>

                                    <div className="header-logout-section">
                                        <div className="header-logout-btn" onClick={handleLogout}>
                                            <LogoutIcon />
                                            <span>退出登录</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                    <div className="header-header-actions">
                        <div className="header-header-action-btn header-news-btn-wrapper" onMouseEnter={handleNewsMouseEnter} onMouseLeave={handleNewsMouseLeave} style={{ position: 'relative' }}>
                            <NewsIcon className="header-action-icon news" />
                            <span>动态</span>
                            <NewsPanel show={showNewsPanel} videos={videoResults} />
                        </div>
                        <div className="header-header-action-btn" onClick={handleMsgClick}>
                            <SendIcon className="header-action-icon" />
                            <span>消息</span>
                        </div>
                        <div className="header-header-action-btn" onClick={handleFavClick}>
                            <HollowFavoriteIcon className="header-action-icon" />
                            <span>收藏</span>
                        </div>
                        <div className="header-header-action-btn" onClick={handleHistoryClick}>
                            <HistoryIcon className="header-action-icon" />
                            <span>历史</span>
                        </div>
                        <div className="header-header-upload-btn" onClick={handleUploadClick}>
                            <UploadIcon className="header-action-icon" />
                            <span>投稿</span>
                        </div>
                    </div>
                </div>
            ) : (
                <div className="header-header-actions">
                    <button className="header-header-upload-btn" onClick={() => setShowLoginModal(true)}>登录</button>
                </div>
            )}
            <LoginModal
                isOpen={showLoginModal}
                onClose={() => setShowLoginModal(false)}
            />
        </header>
    );
};

export default Header;