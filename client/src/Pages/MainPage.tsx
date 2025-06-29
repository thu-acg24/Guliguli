// MainPage.tsx
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
// import { loginPagePath } from "./LoginPage";

export const mainPagePath = "/main"
const MainPage: React.FC = () => {
    const navigate = useNavigate();
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [showSearchModal, setShowSearchModal] = useState(false);
    const [showCategoryModal, setShowCategoryModal] = useState(false);
    const [searchKeyword, setSearchKeyword] = useState("");
    const [categoryTitle, setCategoryTitle] = useState("");
    const [categoryContent, setCategoryContent] = useState("");

    // API call placeholder for search
    const performSearch = () => {
        if (searchKeyword.trim()) {
            setShowSearchModal(true);
            // API call would go here
            // Example: fetch(`/api/search?q=${searchKeyword}`)
            //   .then(response => response.json())
            //   .then(data => setSearchResults(data))
        }
    };

    // API call placeholder for category content
    const loadCategoryContent = (categoryName: string, categoryId: string) => {
        setCategoryTitle(categoryName);
        setCategoryContent(`正在加载${categoryName}分区的内容...`);
        setShowCategoryModal(true);

        // API call would go here
        // Example: fetch(`/api/category/${categoryId}`)
        //   .then(response => response.json())
        //   .then(data => setCategoryContent(data))
    };

    // API call placeholder for hot content
    const loadHotContent = () => {
        setCategoryTitle("热门视频");
        setCategoryContent("正在加载热门视频...");
        setShowCategoryModal(true);

        // API call would go here
        // Example: fetch('/api/hot')
        //   .then(response => response.json())
        //   .then(data => setCategoryContent(data))
    };

    // API call placeholder for video click
    const handleVideoClick = (videoId: string) => {
        // API call would go here for tracking or fetching video details
        // Example: fetch(`/api/video/${videoId}/view`)
        alert(`跳转到视频页面，视频ID: ${videoId}`);
    };

    // API call placeholder for author click
    const handleAuthorClick = (userId: string) => {
        // API call would go here for fetching user profile
        // Example: fetch(`/api/user/${userId}`)
        alert(`跳转到UP主主页，用户ID: ${userId}`);
    };

    // API call placeholder for load more
    const handleLoadMore = () => {
        // API call would go here for pagination
        // Example: fetch('/api/videos?page=2')
        alert('正在加载更多视频...');
    };

    return (
        <div className="main-page">
            {/* Header */}
            <header className="header">
                <div className="logo" onClick={() => alert('跳转到首页')}>BILIBILI</div>
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
            </header>

            {/* Main View */}
            <div className="main-view" onClick={() => alert('跳转到宣传页面')}>
                主页主视图 - 宣传图区域
            </div>

            {/* Category Navigation */}
            <div className="category-nav">
                <div className="hot-btn" onClick={loadHotContent}>热门</div>
                <ul className="category-list">
                    {['动画', '番剧', '国创', '音乐', '舞蹈', '游戏', '知识', '科技', '运动', '汽车', '生活', '美食', '动物圈', '鬼畜', '时尚', '娱乐', '影视', '纪录片', '电影', '电视剧'].map((category) => (
                        <li
                            key={category}
                            className="category-item"
                            onClick={() => loadCategoryContent(category, category.toLowerCase())}
                        >
                            {category}
                        </li>
                    ))}
                </ul>
            </div>

            {/* Video Container */}
            <div className="video-container">
                {/* Video Row with Large Ad */}
                <div className="video-row">
                    <div className="large-ad" onClick={() => alert('跳转到广告链接')}>
                        <img src="https://picsum.photos/800/450?random=101" alt="热门视频广告" className="large-ad-cover" onError={(e) => { e.currentTarget.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMTY5IiB2aWV3Qm94PSIwIDAgMzAwIDE2OSIgZmlsbD0iI2ZmY2NkNSI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIxNjkiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE0IiBmaWxsPSIjZmY3Mjk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIj7lm77niYflnLDor50v5paH5a2XPC90ZXh0Pjwvc3ZnPg=='; }} />
                    </div>
                    <div className="video-grid">
                        {[1, 2, 3, 4].map((i) => (
                            <div key={i} className="video-item" data-video-id={`100${i}`} onClick={(e) => {
                                if (!(e.target as HTMLElement).classList.contains('video-author')) {
                                    handleVideoClick(`100${i}`);
                                }
                            }}>
                                <div className="video-cover-container">
                                    <img src={`https://picsum.photos/300/169?random=${i}`} alt="视频封面" className="video-cover" onError={(e) => { e.currentTarget.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMTY5IiB2aWV3Qm94PSIwIDAgMzAwIDE2OSIgZmlsbD0iI2ZmY2NkNSI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIxNjkiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE0IiBmaWxsPSIjZmY3Mjk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIj7lm77niYflnLDor50v5paH5a2XPC90ZXh0Pjwvc3ZnPg=='; }} />
                                </div>
                                <div className="video-info">
                                    <div className="video-title">{i === 1 ? '这是一个视频标题，可能会比较长，需要显示两行' : `这是另一个视频标题${i}`}</div>
                                    <div className="video-meta">
                                        <span className="video-author" data-user-id={`10${i}`} onClick={(e) => {
                                            e.stopPropagation();
                                            handleAuthorClick(`10${i}`);
                                        }}>
                                            {i === 1 ? 'UP主名称' : `另一个UP主${i}`}
                                        </span>
                                        <span className="video-time">{i === 3 ? '5小时前' : `${i}天前`}</span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Normal Video List */}
                <div className="normal-video-list">
                    {Array.from({ length: 10 }, (_, i) => i + 1).map((i) => (
                        <div key={i} className="video-item" data-video-id={`200${i}`} onClick={(e) => {
                            if (!(e.target as HTMLElement).classList.contains('video-author')) {
                                handleVideoClick(`200${i}`);
                            }
                        }}>
                            <div className="video-cover-container">
                                <img src={`https://picsum.photos/300/169?random=${i + 4}`} alt="视频封面" className="video-cover" onError={(e) => { e.currentTarget.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMTY5IiB2aWV3Qm94PSIwIDAgMzAwIDE2OSIgZmlsbD0iI2ZmY2NkNSI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIxNjkiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE0IiBmaWxsPSIjZmY3Mjk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIj7lm77niYflnLDor50v5paH5a2XPC90ZXh0Pjwvc3ZnPg=='; }} />
                            </div>
                            <div className="video-info">
                                <div className="video-title">视频标题{i}</div>
                                <div className="video-meta">
                                    <span className="video-author" data-user-id={`20${i}`} onClick={(e) => {
                                        e.stopPropagation();
                                        handleAuthorClick(`20${i}`);
                                    }}>
                                        UP主{i}
                                    </span>
                                    <span className="video-time">{i}小时前</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Load More */}
                <div className="load-more" onClick={handleLoadMore}>
                    加载更多...
                </div>
            </div>

            {/* Login Modal */}
            {showLoginModal && (
                <div className="modal" onClick={() => setShowLoginModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <div className="modal-title">登录</div>
                            <div className="modal-close" onClick={() => setShowLoginModal(false)}>&times;</div>
                        </div>
                        <div className="modal-body">
                            <form className="login-form">
                                <div className="form-group">
                                    <label className="form-label">用户名</label>
                                    <input type="text" className="form-input" placeholder="请输入用户名" />
                                </div>
                                <div className="form-group">
                                    <label className="form-label">密码</label>
                                    <input type="password" className="form-input" placeholder="请输入密码" />
                                </div>
                                <button type="button" className="login-btn" onClick={() => {
                                    // API call would go here for login
                                    // Example: fetch('/api/login', { method: 'POST', body: JSON.stringify({ username, password }) })
                                    // navigate(loginPagePath);
                                }}>
                                    登录
                                </button>
                                <div className="register-link">
                                    还没有账户？<a href="#" onClick={(e) => {
                                        e.preventDefault();
                                        alert('跳转到注册页面');
                                        setShowLoginModal(false);
                                    }}>点击注册</a>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}

            {/* Search Modal */}
            {showSearchModal && (
                <div className="modal" onClick={() => setShowSearchModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <div className="modal-title">搜索结果</div>
                            <div className="modal-close" onClick={() => setShowSearchModal(false)}>&times;</div>
                        </div>
                        <div className="modal-body">
                            <p>正在搜索：<span>{searchKeyword}</span></p>
                            <div>
                                <div style={{ marginTop: '15px', padding: '10px', background: '#f5f5f5', borderRadius: '4px' }}>
                                    <p>搜索到5个与"{searchKeyword}"相关的视频</p>
                                    <ul style={{ marginTop: '10px' }}>
                                        <li style={{ padding: '5px 0', borderBottom: '1px solid #eee' }}>{searchKeyword}相关视频1</li>
                                        <li style={{ padding: '5px 0', borderBottom: '1px solid #eee' }}>{searchKeyword}相关视频2</li>
                                        <li style={{ padding: '5px 0', borderBottom: '1px solid #eee' }}>{searchKeyword}相关视频3</li>
                                        <li style={{ padding: '5px 0', borderBottom: '1px solid #eee' }}>{searchKeyword}相关视频4</li>
                                        <li style={{ padding: '5px 0' }}>{searchKeyword}相关视频5</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Category Modal */}
            {showCategoryModal && (
                <div className="modal" onClick={() => setShowCategoryModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <div className="modal-title">{categoryTitle}</div>
                            <div className="modal-close" onClick={() => setShowCategoryModal(false)}>&times;</div>
                        </div>
                        <div className="modal-body">
                            <p>
                                <div style={{ marginTop: '10px' }}>
                                    <h3 style={{ color: '#fb7299', marginBottom: '10px' }}>{categoryTitle}分区热门视频</h3>
                                    <ul>
                                        <li style={{ padding: '8px 0', borderBottom: '1px solid #eee' }}>{categoryTitle}视频1</li>
                                        <li style={{ padding: '8px 0', borderBottom: '1px solid #eee' }}>{categoryTitle}视频2</li>
                                        <li style={{ padding: '8px 0', borderBottom: '1px solid #eee' }}>{categoryTitle}视频3</li>
                                        <li style={{ padding: '8px 0' }}>{categoryTitle}视频4</li>
                                    </ul>
                                </div>
                            </p>
                        </div>
                    </div>
                </div>
            )}

            {/* CSS - Maintained from original HTML */}
            <style>{`
                /* 全局样式 */
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                    font-family: "Microsoft YaHei", sans-serif;
                }
                
                body {
                    background-color: #f4f4f4;
                    color: #333;
                }
                
                a {
                    text-decoration: none;
                    color: inherit;
                }
                
                ul {
                    list-style: none;
                }
                
                /* 主页栏样式 */
                .header {
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 60px;
                    background-color: white;
                    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
                    display: flex;
                    align-items: center;
                    padding: 0 20px;
                    z-index: 1000;
                }
                
                .logo {
                    width: 120px;
                    height: 40px;
                    background-color: #fb7299;
                    border-radius: 4px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: white;
                    font-weight: bold;
                    margin-right: 40px;
                    cursor: pointer;
                }
                
                .search-container {
                    position: absolute;
                    left: 50%;
                    transform: translateX(-50%);
                    width: 400px;
                }
                
                .search-box {
                    width: 100%;
                    height: 40px;
                    display: flex;
                }
                
                .search-input {
                    flex: 1;
                    height: 100%;
                    padding: 0 15px;
                    border: 1px solid #e7e7e7;
                    border-right: none;
                    border-radius: 4px 0 0 4px;
                    outline: none;
                    font-size: 14px;
                }
                
                .search-input:focus {
                    border-color: #fb7299;
                }
                
                .search-btn {
                    width: 60px;
                    height: 100%;
                    background-color: #fb7299;
                    border: none;
                    border-radius: 0 4px 4px 0;
                    color: white;
                    cursor: pointer;
                    font-size: 16px;
                }
                
                .user-actions {
                    display: flex;
                    align-items: center;
                    margin-left: auto;
                }
                
                .user-avatar {
                    width: 36px;
                    height: 36px;
                    border-radius: 50%;
                    background-color: #e7e7e7;
                    cursor: pointer;
                    overflow: hidden;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                
                .user-avatar img {
                    width: 100%;
                    height: 100%;
                    object-fit: cover;
                }
                
                .action-btn {
                    margin-left: 20px;
                    font-size: 14px;
                    color: #666;
                    cursor: pointer;
                    padding: 8px 10px;
                    border-radius: 4px;
                }
                
                .action-btn:hover {
                    color: #fb7299;
                    background-color: #f5f5f5;
                }
                
                .upload-btn {
                    margin-left: 20px;
                    padding: 8px 15px;
                    background-color: #fb7299;
                    color: white;
                    border-radius: 4px;
                    font-size: 14px;
                    cursor: pointer;
                }
                
                .upload-btn:hover {
                    background-color: #ff85ad;
                }
                
                /* 主页主视图 */
                .main-view {
                    width: 100%;
                    height: 400px;
                    background-color: #ddd;
                    margin-top: 60px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    color: #666;
                    font-size: 24px;
                    background-image: linear-gradient(45deg, #fb7299, #ff85ad, #ff9dbd);
                    color: white;
                    cursor: pointer;
                }
                
                /* 分区导航栏 */
                .category-nav {
                    width: 100%;
                    background-color: white;
                    padding: 15px 20px;
                    border-bottom: 1px solid #e7e7e7;
                    display: flex;
                    align-items: center;
                }
                
                .hot-btn {
                    padding: 8px 20px;
                    background-color: #fb7299;
                    color: white;
                    border-radius: 4px;
                    margin-right: 15px;
                    font-size: 14px;
                    cursor: pointer;
                    font-weight: bold;
                }
                
                .category-list {
                    display: flex;
                    flex-wrap: wrap;
                }
                
                .category-item {
                    padding: 8px 15px;
                    margin-right: 10px;
                    font-size: 14px;
                    cursor: pointer;
                    border-radius: 4px;
                    border: 1px solid #e7e7e7;
                    background-color: #f7f7f7;
                    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
                }
                
                .category-item:hover {
                    background-color: #f0f0f0;
                    color: #fb7299;
                }
                
                /* 视频栏 */
                .video-container {
                    width: 100%;
                    max-width: 1400px;
                    margin: 0 auto;
                    padding: 20px;
                }
                
                .video-row {
                    display: flex;
                    margin-bottom: 20px;
                }
                
                .large-ad {
                    width: 60%;
                    margin-right: 20px;
                    background-color: #ddd;
                    border-radius: 6px;
                    overflow: hidden;
                    position: relative;
                }
                
                .large-ad-cover {
                    width: 100%;
                    height: 100%;
                    object-fit: cover;
                    display: block;
                }
                
                .video-grid {
                    width: 40%;
                    display: grid;
                    grid-template-columns: repeat(2, 1fr);
                    grid-gap: 10px;
                }
                
                /* 视频项固定尺寸 */
                .video-item {
                    background-color: white;
                    border-radius: 6px;
                    overflow: hidden;
                    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
                    cursor: pointer;
                }
                
                /* 固定封面框尺寸 */
                .video-cover-container {
                    position: relative;
                    width: 100%;
                    padding-bottom: 56.25%; /* 16:9 比例 */
                    overflow: hidden;
                }
                
                .video-cover {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    object-fit: cover;
                    display: block;
                }
                
                .video-info {
                    padding: 10px;
                }
                
                .video-title {
                    font-size: 14px;
                    margin-bottom: 5px;
                    display: -webkit-box;
                    -webkit-line-clamp: 2;
                    -webkit-box-orient: vertical;
                    overflow: hidden;
                    height: 40px;
                }
                
                .video-meta {
                    display: flex;
                    font-size: 12px;
                    color: #999;
                }
                
                .video-author {
                    margin-right: 10px;
                    cursor: pointer;
                }
                
                .video-author:hover {
                    color: #fb7299;
                }
                
                /* 普通视频列表 */
                .normal-video-list {
                    display: grid;
                    grid-template-columns: repeat(5, 1fr);
                    gap: 15px;
                    margin-bottom: 20px;
                }
                
                /* 弹窗样式 */
                .modal {
                    position: fixed;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    background-color: rgba(0, 0, 0, 0.5);
                    z-index: 2000;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                
                .modal-content {
                    background-color: white;
                    width: 400px;
                    border-radius: 8px;
                    overflow: hidden;
                    animation: modalFadeIn 0.3s;
                }
                
                @keyframes modalFadeIn {
                    from {
                        opacity: 0;
                        transform: translateY(-50px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
                
                .modal-header {
                    padding: 15px 20px;
                    border-bottom: 1px solid #e7e7e7;
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                }
                
                .modal-title {
                    font-size: 18px;
                    font-weight: bold;
                }
                
                .modal-close {
                    font-size: 24px;
                    cursor: pointer;
                    color: #999;
                }
                
                .modal-body {
                    padding: 20px;
                }
                
                .login-form {
                    display: flex;
                    flex-direction: column;
                }
                
                .form-group {
                    margin-bottom: 15px;
                }
                
                .form-label {
                    display: block;
                    margin-bottom: 5px;
                    font-size: 14px;
                }
                
                .form-input {
                    width: 100%;
                    padding: 10px;
                    border: 1px solid #e7e7e7;
                    border-radius: 4px;
                    font-size: 14px;
                }
                
                .form-input:focus {
                    border-color: #fb7299;
                }
                
                .login-btn {
                    padding: 10px;
                    background-color: #fb7299;
                    color: white;
                    border: none;
                    border-radius: 4px;
                    font-size: 16px;
                    cursor: pointer;
                    margin-top: 10px;
                }
                
                .login-btn:hover {
                    background-color: #ff85ad;
                }
                
                .register-link {
                    text-align: center;
                    margin-top: 15px;
                    font-size: 14px;
                }
                
                .register-link a {
                    color: #fb7299;
                    text-decoration: underline;
                }
                
                /* 加载更多 */
                .load-more {
                    text-align: center;
                    padding: 20px 0;
                    color: #999;
                    cursor: pointer;
                }
                
                .load-more:hover {
                    color: #fb7299;
                }
            `}</style>
        </div>
    );
};

export default MainPage;