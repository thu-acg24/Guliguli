import React, { useState } from "react";
import { useNavigateAudit, useNavigateMain, useNavigateHome, HomePageTab, useNavigateMember, useNavigateMessage, useNavigateSearch } from "Globals/Navigate";
import iconSrc from "Images/LOGO.png";
import LoginModal from "Components/LoginModal/LoginModal";
import { useUserToken, setUserToken, useUserInfo, useUserStat, useUserID } from "Globals/GlobalStore";
import { useUserRole } from "Hooks/useUserRole";
import { UserRole } from "Plugins/UserService/Objects/UserRole";
import { LogoutMessage } from "Plugins/UserService/APIs/LogoutMessage";
import { PersonCenterIcon, LogoutIcon } from "Images/Icons";
import DEFAULT_AVATAR from "Images/DefaultAvatar.jpg";
import { SendIcon, HollowFavoriteIcon, HistoryIcon, UploadIcon,SearchIcon } from "Images/Icons";
import "./Header.css";

const Header: React.FC<{ usetransparent?: boolean, transparent?: boolean, hideSearch?: boolean }> = ({ usetransparent = false, transparent = false, hideSearch = false }) => {
    const { navigateAudit } = useNavigateAudit();
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
    const { userRole } = useUserRole();
    // 跳转函数
    const handleAvatarClick = async () => {
        navigateHome(userID);
    }
    // 审核中心跳转暂保留原 navigate，或根据业务需求补充封装
    const handleAuditClick = async () => {
        navigateAudit();
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
                                <div className="header-zoomed-avatar">
                                    <img
                                        src={userInfo?.avatarPath || DEFAULT_AVATAR}
                                        alt="用户头像"
                                    />
                                </div>
                                <div className="header-user-panel-content">
                                    {/* 移除原大头像区域，昵称居中 */}
                                    <div className="header-user-nickname">
                                        {userInfo?.username || "用户"}
                                    </div>

                                    <div className="header-user-stats">
                                        <div className="header-stat-item">
                                            <div className="header-stat-number">{userStat?.followingCount || 0}</div>
                                            <div className="header-stat-label">关注</div>
                                        </div>
                                        <div className="header-stat-item">
                                            <div className="header-stat-number">{userStat?.followerCount || 0}</div>
                                            <div className="header-stat-label">粉丝</div>
                                        </div>
                                        <div className="header-stat-item">
                                            <div className="header-stat-number">0</div>
                                            <div className="header-stat-label">视频</div>
                                        </div>
                                    </div>

                                    <div className="header-panel-links">
                                        <div className="header-panel-link-item" onClick={handleAvatarClick}>
                                            <PersonCenterIcon />
                                            <span>个人中心</span>
                                        </div>
                                    </div>

                                    {userRole === UserRole.auditor &&
                                        <div className="header-panel-links">
                                            <div className="header-panel-link-item" onClick={handleAuditClick}>
                                                <PersonCenterIcon />
                                                <span>审核中心</span>
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