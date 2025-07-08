import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import iconSrc from "Images/LOGO.png";
import LoginModal from "Components/LoginModal/LoginModal";
import { useUserToken, setUserToken, useUserInfo, useUserStat, useUserID } from "Globals/GlobalStore";
import { useUserRole } from "Hooks/useUserRole";
import { UserRole } from "Plugins/UserService/Objects/UserRole";
import { LogoutMessage } from "Plugins/UserService/APIs/LogoutMessage";
import { PersonCenterIcon, LogoutIcon } from "./Icons";
import { DEFAULT_AVATAR } from "Components/DefaultAvatar";
import "./Header.css";

import { mainPagePath } from "Pages/MainPage/MainPage";
import { messagePagePath } from "Pages/MessagePage/MessagePage";
import { memberPagePath } from "Pages/MemberPage/MemberPage";

const Header: React.FC<{ usetransparent?: boolean, transparent?: boolean }> = ({ usetransparent = false, transparent = false }) => {
    const navigate = useNavigate();
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
        navigate(`/home/${userID}`);
    }
    const handleAuditClick = async () => {
        navigate(`/audit`);
    }

    // 处理头像鼠标悬浮
    const handleAvatarMouseEnter = async () => {
        setShowUserPanel(true);
    };

    const handleAvatarMouseLeave = () => {
        setShowUserPanel(false);
    };


    const handleMsgClick = async () => {
        navigate(messagePagePath);
    };
    const handleFavClick = async () => {
        navigate(`/home/${userID}/favorites`);
    };
    const handleHistoryClick = async () => {
        navigate(`/home/${userID}/history`);
    };
    const handleUploadClick = async () => {
        navigate(`${memberPagePath}/upload`);
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
        if (searchKeyword.trim()) {
            // 这里可以弹窗或跳转
            alert(`搜索：${searchKeyword}`);
        }
    };

    return (
        <header className={`header-header ${usetransparent ? 'usetransparent' : ''} ${transparent ? 'transparent' : ''}`}>
            <div className="header-logo" onClick={() => {
                navigate(mainPagePath);
                window.scrollTo({ top: 0, behavior: "smooth" });
            }}>
                <img src={iconSrc} alt="GULIGULI" className="header-logo-icon" />
            </div>
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
                    <button className="header-search-btn" onClick={performSearch}>搜索</button>
                </div>
            </div>
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
                                <div className="header-user-panel-content">
                                    <div className="header-user-basic-info">
                                        <div className="header-user-avatar-large">
                                            <img
                                                src={userInfo?.avatarPath || DEFAULT_AVATAR}
                                                alt="用户头像"
                                            />
                                        </div>
                                        <div className="header-user-nickname">
                                            {userInfo?.username || "用户"}
                                        </div>
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
                    <div className="header-header-action-btn" onClick={handleMsgClick}>消息</div>
                    <div className="header-header-action-btn" onClick={handleFavClick}>收藏</div>
                    <div className="header-header-action-btn" onClick={handleHistoryClick}>历史</div>
                    <div className="header-header-upload-btn" onClick={handleUploadClick}>投稿</div>
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