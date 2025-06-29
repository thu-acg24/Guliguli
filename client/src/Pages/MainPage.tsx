// MainPage.tsx
import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../Components/Header";

export const mainPagePath = "/"
const MainPage: React.FC = () => {
    const navigate = useNavigate();
    const [showCategoryModal, setShowCategoryModal] = useState(false);
    const [categoryTitle, setCategoryTitle] = useState("");
    const [categoryContent, setCategoryContent] = useState("");

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
            <Header />

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

        </div>
    );
};

export default MainPage;