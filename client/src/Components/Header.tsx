import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import iconSrc from "../icon.png";
import LoginModal from "./LoginModal";

const mainPagePath = "/";

const Header: React.FC = () => {
    const navigate = useNavigate();
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [searchKeyword, setSearchKeyword] = useState("");

    const performSearch = () => {
        if (searchKeyword.trim()) {
            // 这里可以弹窗或跳转
            alert(`搜索：${searchKeyword}`);
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
            <div className="user-actions">
                <div className="user-avatar" onClick={() => setShowLoginModal(true)}>
                    <img src="data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWRlLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI3IiByPSI0Ii8+PC9zdmc+" alt="用户头像" />
                </div>
                <div className="action-btn" onClick={() => setShowLoginModal(true)}>消息</div>
                <div className="action-btn" onClick={() => setShowLoginModal(true)}>动态</div>
                <div className="action-btn" onClick={() => setShowLoginModal(true)}>收藏</div>
                <div className="action-btn" onClick={() => setShowLoginModal(true)}>历史</div>
                <div className="upload-btn" onClick={() => setShowLoginModal(true)}>投稿</div>
            </div>
            <LoginModal
                isOpen={showLoginModal}
                onClose={() => setShowLoginModal(false)}
            />
        </header>
    );
};

export default Header;