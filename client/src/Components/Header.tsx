import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import iconSrc from "../icon.png";
import LoginModal from "./LoginModal";
import { useUserToken } from "../Globals/GlobalStore";
import { GetUIDByTokenMessage } from "../Plugins/UserService/APIs/GetUIDByTokenMessage";

import { mainPagePath } from "../Pages/MainPage";

const Header: React.FC = () => {
    const navigate = useNavigate();
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [searchKeyword, setSearchKeyword] = useState("");
    const userToken = useUserToken();

    // 校验token有效性
    const checkLoginValid = async (): Promise<boolean> => {
        if (!userToken) return false;
        return new Promise((resolve) => {
            try {
                new GetUIDByTokenMessage(userToken).send(
                    (info: string) => {
                        resolve(true)
                    },
                    (error: string) => {
                        console.error("Token校验失败:", error);
                        resolve(false);
                    }
                );
            } catch {
                resolve(false);
            }
        });
    };

    const performSearch = () => {
        if (searchKeyword.trim()) {
            // 这里可以弹窗或跳转
            alert(`搜索：${searchKeyword}`);
        }
    };

    // 跳转函数
    const handleAvatarClick = async () => {
        if (await checkLoginValid()) {
            // TODO: 跳转到个人中心页面
        } else {
            setShowLoginModal(true);
        }
    };
    const handleMsgClick = async () => {
        if (await checkLoginValid()) {
            // TODO: 跳转到消息页面
        } else {
            setShowLoginModal(true);
        }
    };
    const handleDynamicClick = async () => {
        if (await checkLoginValid()) {
            // TODO: 跳转到动态页面
        } else {
            setShowLoginModal(true);
        }
    };
    const handleFavClick = async () => {
        if (await checkLoginValid()) {
            // TODO: 跳转到收藏页面
        } else {
            setShowLoginModal(true);
        }
    };
    const handleHistoryClick = async () => {
        if (await checkLoginValid()) {
            // TODO: 跳转到历史页面
        } else {
            setShowLoginModal(true);
        }
    };
    const handleUploadClick = async () => {
        if (await checkLoginValid()) {
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
                <div className="user-actions">
                    <div className="user-avatar" onClick={handleAvatarClick}>
                        <img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWRlLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI3IiByPSI0Ii8+PC9zdmc+" alt="用户头像" />
                    </div>
                    <div className="action-btn" onClick={handleMsgClick}>消息</div>
                    <div className="action-btn" onClick={handleDynamicClick}>动态</div>
                    <div className="action-btn" onClick={handleFavClick}>收藏</div>
                    <div className="action-btn" onClick={handleHistoryClick}>历史</div>
                    <div className="upload-btn" onClick={handleUploadClick}>投稿</div>
                </div>
            ) : (
                <div className="user-actions">
                    <button className="login-btn" onClick={() => setShowLoginModal(true)}>登录</button>
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