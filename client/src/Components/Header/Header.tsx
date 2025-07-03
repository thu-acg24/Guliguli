import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import iconSrc from "./icon.png";
import LoginModal from "Components/LoginModal/LoginModal";
import { useUserToken, setUserToken } from "Globals/GlobalStore";
import { GetUIDByTokenMessage } from "Plugins/UserService/APIs/GetUIDByTokenMessage";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import { QueryUserStatMessage } from "Plugins/UserService/APIs/QueryUserStatMessage";
import { LogoutMessage } from "Plugins/UserService/APIs/LogoutMessage";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { UserStat } from "Plugins/UserService/Objects/UserStat";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { PersonCenterIcon, LogoutIcon } from "./Icons";
import { DEFAULT_AVATAR } from "Components/DefaultAvatar";
import "./Header.css"; 

import { mainPagePath } from "Pages/MainPage/MainPage";
import { messagePagePath } from "Pages/MessagePage/MessagePage";

const Header: React.FC = () => {
    const navigate = useNavigate();
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [searchKeyword, setSearchKeyword] = useState("");
    const [showUserPanel, setShowUserPanel] = useState(false);
    const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
    const [userStat, setUserStat] = useState<UserStat | null>(null);
    const userToken = useUserToken();

    useEffect(() => {
        const fetchUserData = async () => {
            const userID = await getUIDByToken();
            if (userID !== null) {
                await fetchUserInfo(userID);
                await fetchUserStat(userID);
            } else {
                setUserInfo(null);
                setUserStat(null);
            }
        };
        fetchUserData();
    }, [userToken]);

    // 校验token有效性
    const getUIDByToken = async (): Promise<number | null> => {
        // return 1
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
                (info: string) => {
                    const userInfo = JSON.parse(info);
                    setUserInfo(userInfo);
                },
                (e: string) => {
                    console.error("获取用户信息失败:", e);
                }
            );
        } catch (e) {
            console.error("获取用户信息异常:", e.message);
        }
    };

    // 获取用户统计信息
    const fetchUserStat = async (userID: number) => {
        try {
            new QueryUserStatMessage(userID).send(
                (info: string) => {
                    const userStat = JSON.parse(info);
                    setUserStat(userStat);
                },
                (e: string) => {
                    console.error("获取用户统计信息失败:", e);
                }
            );
        } catch (e) {
            console.error("获取用户统计信息异常:", e.message);
        }
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

    // 跳转函数
    const handleAvatarClick = async () => {
        const userID = await getUIDByToken();
        if (userID !== null) {
            navigate(`/home/${userID}`);
        } else {
            setShowLoginModal(true);
        }
    }

    // 处理头像鼠标悬浮
    const handleAvatarMouseEnter = async () => {
        setShowUserPanel(true);
        // await fetchUserInfo();
    };

    const handleAvatarMouseLeave = () => {
        setShowUserPanel(false);
    };

    const handleMsgClick = async () => {
        const userID = await getUIDByToken();
        if (userID !== null) {
            navigate(messagePagePath);
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
        <header className="header-header">
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
            {userToken ? (
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
                    <div className="header-header-action-btn" onClick={handleDynamicClick}>动态</div>
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