import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import iconSrc from "../icon.png";
import LoginModal from "./LoginModal";
import { useUserToken, setUserToken } from "../Globals/GlobalStore";
import { GetUIDByTokenMessage } from "../Plugins/UserService/APIs/GetUIDByTokenMessage";
import { QueryUserInfoMessage } from "../Plugins/UserService/APIs/QueryUserInfoMessage";
import { QueryUserStatMessage } from "../Plugins/UserService/APIs/QueryUserStatMessage";
import { LogoutMessage } from "../Plugins/UserService/APIs/LogoutMessage";
import { UserInfo } from "../Plugins/UserService/Objects/UserInfo";
import { UserStat } from "../Plugins/UserService/Objects/UserStat";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { PersonCenterIcon, UploadManageIcon, LogoutIcon } from "./Icons";
import { DEFAULT_AVATAR } from "./DefaultAvatar";

import { mainPagePath } from "../Pages/MainPage";
import { messagePagePath } from "../Pages/MessagePage";

const Header: React.FC = () => {
    const navigate = useNavigate();
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [searchKeyword, setSearchKeyword] = useState("");
    const [showUserPanel, setShowUserPanel] = useState(false);
    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
    const [userStat, setUserStat] = useState<UserStat | null>(null);
    const [currentUserID, setCurrentUserID] = useState<number | null>(1);
    const userToken = useUserToken();

    // 校验token有效性
    const getUIDByToken = async (): Promise<number | null> => {
        if (!userToken) return null;
        return new Promise((resolve) => {
            try {
                new GetUIDByTokenMessage(userToken).send(
                    (info: string) => {
                        const user_id = JSON.parse(info);
                        resolve(user_id)
                    },
                    (e: string) => {
                        console.error("Token校验失败:", e);
                        materialAlertError(`Token校验失败`, "", () => {
                            setUserToken("");
                            resolve(null);
                        });
                    }
                );
            } catch (e) {
                console.error("Token校验异常:", e.message);
                materialAlertError(`Token校验失败`, "", () => {
                    setUserToken("");
                    resolve(null);
                });
            }
        });
    };

    // 获取用户信息
    const fetchUserInfo = async (userID: number) => {
        try {
            new QueryUserInfoMessage(userID).send(
                (info: UserInfo) => {
                    setUserInfo(info);
                },
                (e: string) => {
                    console.error("获取用户信息失败:", e);
                }
            );
        } catch (e) {
            console.error("获取用户信息异常:", e);
        }
    };

    // 获取用户统计信息
    const fetchUserStat = async (userID: number) => {
        try {
            new QueryUserStatMessage(userID).send(
                (stat: UserStat) => {
                    setUserStat(stat);
                },
                (e: string) => {
                    console.error("获取用户统计信息失败:", e);
                }
            );
        } catch (e) {
            console.error("获取用户统计信息异常:", e);
        }
    };

    // 处理登出
    const handleLogout = async () => {
        if (!userToken) return;
        try {
            new LogoutMessage(userToken).send(
                () => {
                    setUserToken("");
                    setUserInfo(null);
                    setUserStat(null);
                    setCurrentUserID(null);
                    setShowUserPanel(false);
                },
                (e: string) => {
                    console.error("登出失败:", e);
                    // 即使服务器登出失败，也清除本地token
                    setUserToken("");
                    setUserInfo(null);
                    setUserStat(null);
                    setCurrentUserID(null);
                    setShowUserPanel(false);
                }
            );
        } catch (e) {
            console.error("登出异常:", e);
            // 即使登出异常，也清除本地状态
            setUserToken("");
            setUserInfo(null);
            setUserStat(null);
            setCurrentUserID(null);
            setShowUserPanel(false);
        }
    };

    const performSearch = () => {
        if (searchKeyword.trim()) {
            // 这里可以弹窗或跳转
            alert(`搜索：${searchKeyword}`);
        }
    };

    // 跳转函数
    const handleAvatarClick = async () => {
        if (await getUIDByToken() !== null) {
            // TODO: 跳转到个人中心页面
        } else {
            setShowLoginModal(true);
        }
    };

    // 处理头像鼠标悬浮
    const handleAvatarMouseEnter = async () => {
        setShowUserPanel(true);
        // await fetchUserInfo();
    };

    const handleAvatarMouseLeave = () => {
        setShowUserPanel(false);
    };

    const handleMsgClick = async () => {
        const userId = await getUIDByToken();
        if (userId !== null) {
            navigate(`/message/`);
        } else {
            setShowLoginModal(true);
        }
    };
    const handleDynamicClick = async () => {
        if (await getUIDByToken() !== null) {
            // TODO: 跳转到动态页面
        } else {
            setShowLoginModal(true);
        }
    };
    const handleFavClick = async () => {
        if (await getUIDByToken() !== null) {
            // TODO: 跳转到收藏页面
        } else {
            setShowLoginModal(true);
        }
    };
    const handleHistoryClick = async () => {
        if (await getUIDByToken() !== null) {
            // TODO: 跳转到历史页面
        } else {
            setShowLoginModal(true);
        }
    };
    const handleUploadClick = async () => {
        if (await getUIDByToken() !== null) {
            // TODO: 跳转到投稿页面
        } else {
            setShowLoginModal(true);
        }
    };

    return (
        <header className="header">
            <div className="logo" onClick={() => {
                navigate(mainPagePath);
                window.scrollTo({ top: 0, behavior: "smooth" });
            }}>
                <img src={iconSrc} alt="GULIGULI" className="logo-icon" />
            </div>
            <div className="search-container">
                <div className="search-box">
                    <input
                        type="text"
                        className="search-input"
                        placeholder="搜索视频、UP主"
                        value={searchKeyword}
                        onChange={(e) => setSearchKeyword(e.target.value)}
                        onKeyPress={(e) => e.key === 'Enter' && performSearch()}
                    />
                    <button className="search-btn" onClick={performSearch}>搜索</button>
                </div>
            </div>
            {userToken ? (
                <div className="header-actions">
                    <div
                        className="user-avatar-container"
                        onMouseEnter={handleAvatarMouseEnter}
                        onMouseLeave={handleAvatarMouseLeave}
                    >
                        <div className="user-avatar" onClick={handleAvatarClick}>
                            <img
                                src={userInfo?.avatarPath || DEFAULT_AVATAR}
                                alt="用户头像"
                            />
                        </div>
                        {showUserPanel && (
                            <div className="user-panel-popover">
                                <div className="user-panel-content">
                                    <div className="user-basic-info">
                                        <div className="user-avatar-large">
                                            <img
                                                src={userInfo?.avatarPath || "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWRlLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI3IiByPSI0Ii8+PC9zdmc+"}
                                                alt="用户头像"
                                            />
                                        </div>
                                        <div className="user-nickname">
                                            {userInfo?.username || "用户"}
                                        </div>
                                    </div>

                                    <div className="user-stats">
                                        <div className="stat-item">
                                            <div className="stat-number">{userStat?.followingCount || 0}</div>
                                            <div className="stat-label">关注</div>
                                        </div>
                                        <div className="stat-item">
                                            <div className="stat-number">{userStat?.followerCount || 0}</div>
                                            <div className="stat-label">粉丝</div>
                                        </div>
                                        <div className="stat-item">
                                            <div className="stat-number">0</div>
                                            <div className="stat-label">视频</div>
                                        </div>
                                    </div>

                                    <div className="panel-links">
                                        <div className="panel-link-item" onClick={handleAvatarClick}>
                                            <PersonCenterIcon />
                                            <span>个人中心</span>
                                        </div>
                                        <div className="panel-link-item" onClick={handleUploadClick}>
                                            <UploadManageIcon />
                                            <span>投稿管理</span>
                                        </div>
                                    </div>

                                    <div className="panel-divider"></div>

                                    <div className="logout-section">
                                        <div className="logout-btn" onClick={handleLogout}>
                                            <LogoutIcon />
                                            <span>退出登录</span>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                    <div className="header-action-btn" onClick={handleMsgClick}>消息</div>
                    <div className="header-action-btn" onClick={handleDynamicClick}>动态</div>
                    <div className="header-action-btn" onClick={handleFavClick}>收藏</div>
                    <div className="header-action-btn" onClick={handleHistoryClick}>历史</div>
                    <div className="header-upload-btn" onClick={handleUploadClick}>投稿</div>
                </div>
            ) : (
                <div className="header-actions">
                    <button className="header-upload-btn" onClick={() => setShowLoginModal(true)}>登录</button>
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