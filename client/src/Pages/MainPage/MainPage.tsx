import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useNavigateHome, useNavigateVideo } from "Globals/Navigate";
import Header from "Components/Header/Header";
import MainPicSrc from "Images/MainPic.jpg";
import { useUserToken } from "Globals/GlobalStore";
import { getRecommendedVideos, SimpleVideo } from "Components/RecommendVideoService";
import { dateformatTime } from "Components/Formatter";
import DefaultCover from "Images/DefaultCover.jpg";
import Advertisement from "Images/Advertisement.jpg"
import { RefreshIcon } from "Images/Icons";
import "./MainPage.css";


export const mainPagePath = "/mainpage";
export function useNavigateMain() {
    const navigate = useNavigate();
    const navigateMain = useCallback(() => {
        navigate(mainPagePath);
    }, [navigate]);
    return { navigateMain };
}

const MainPage: React.FC = () => {
    const { navigateHome } = useNavigateHome();
    const { navigateVideo } = useNavigateVideo();
    const [showCategoryModal, setShowCategoryModal] = useState(false);
    const [categoryTitle, setCategoryTitle] = useState("");
    const [recommendvideosInfo, setRecommendvideosInfo] = useState<SimpleVideo[]>([]);
    const [categoryContent, setCategoryContent] = useState("");
    const [videosisloading, setVideosisloading] = useState(false);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const userToken = useUserToken();

    

    const loadCategoryContent = (categoryName: string, categoryId: string) => {
        setCategoryTitle(categoryName);
        setCategoryContent(`正在加载${categoryName}分区的内容...`);
        setShowCategoryModal(true);
    };
    useEffect(() => {
      handleRefresh()
    }, []);
    const handleRefresh = useCallback(async () => {
        if(videosisloading)return;
        setIsRefreshing(true);
        setTimeout(() => {
            setIsRefreshing(false);
        }, 500);
        try {
            setVideosisloading(true);
            const videos = await getRecommendedVideos(userToken ? userToken : null, null, 16);
            setRecommendvideosInfo(videos);
        } finally {
            setVideosisloading(false);
        }
    }, [userToken]);
    const loadHotContent = () => {
        setCategoryTitle("热门视频");
        setCategoryContent("正在加载热门视频...");
        setShowCategoryModal(true);
    };


    const handleVideoClick = (videoId: number) => {
        if (videoId > 0) {
            navigateVideo(videoId);
        }
    };

    const handleAuthorClick = (userID: number) => {
        if (userID > 0) {
            navigateHome(userID);
        }
    };

    const handleLoadMore = () => {
        // 加载更多逻辑
    };

    const [isAtTop, setIsAtTop] = useState(window.scrollY === 0);

    const handleScroll = () => {
        const currentIsAtTop = window.scrollY === 0;
        if (currentIsAtTop !== isAtTop) {
            setIsAtTop(currentIsAtTop);
        }
    };

    useEffect(() => {
        window.addEventListener("scroll", handleScroll);
        return () => window.removeEventListener("scroll", handleScroll);
    }, [isAtTop]);


    return (
        <div className="main-main-page">
            <Header usetransparent={true} transparent={isAtTop} />

            <div className="main-main-view">
                <img src={MainPicSrc} alt="主视图" className="main-large-ad-cover" />
            </div>

            <div className="main-category-nav">
                <div className="main-hot-btn" onClick={loadHotContent}>热门</div>
                <ul className="main-category-list">
                    {['动画', '番剧', '国创', '音乐', '舞蹈', '游戏', '知识', '科技', '运动', '汽车', '生活', '美食', '动物圈', '鬼畜', '时尚', '娱乐', '影视'].map((category) => (
                        <li
                            key={category}
                            className="main-category-item"
                            onClick={() => loadCategoryContent(category, category.toLowerCase())}
                        >
                            {category}
                        </li>
                    ))}
                </ul>
            </div>
<div className="main-refresh-wrapper">
            <div className="main-video-container">
                <div className="main-video-row main-video-row-2x5">
                    <div className="main-large-ad main-large-ad-small">
                        <img src={Advertisement} alt="热门视频广告" className="main-large-ad-cover" onError={(e) => { e.currentTarget.src = DefaultCover; }} />
                    </div>
                    {/* 右侧2行3列视频，整体2行5列，广告占1-2列6格视频占3-5列 */}
                    {[0, 1, 2, 3, 4, 5].map((i) => (
                        <div key={i} className="main-video-item main-video-item-2x5" data-video-id={recommendvideosInfo[i]?.videoID} onClick={(e) => {
                            if (!(e.target as HTMLElement).classList.contains('video-author') && recommendvideosInfo[i]?.videoID) {
                                handleVideoClick(recommendvideosInfo[i].videoID);
                            }
                        }}>
                            <div className="main-video-cover-container">
                                <img
                                    src={recommendvideosInfo[i]?.cover || DefaultCover}
                                    alt="视频封面"
                                    className="main-video-cover"
                                    onError={(e) => { e.currentTarget.src = DefaultCover; }}
                                />
                            </div>
                            <div className="main-video-info">
                                <div className="main-video-title">{recommendvideosInfo[i]?.title || "暂无视频"}</div>
                                <div className="main-video-meta">
                                    <span
                                        className="main-video-author"
                                        data-user-id={recommendvideosInfo[i]?.uploaderID}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            if (recommendvideosInfo[i]?.uploaderID) {
                                                handleAuthorClick(recommendvideosInfo[i].uploaderID);
                                            }
                                        }}
                                    >
                                        UP主：{recommendvideosInfo[i]?.uploaderInfo?.username || "未知用户"}
                                    </span>
                                    <span className="main-video-time">
                                        {recommendvideosInfo[i]?.uploadTime ? dateformatTime(recommendvideosInfo[i].uploadTime) : ""}
                                    </span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                <div className="main-normal-video-list">
                    {Array.from({ length: 10 }, (_, i) => i + 6).map((i) => (
                        <div key={i} className="main-video-item" data-video-id={recommendvideosInfo[i]?.videoID} onClick={(e) => {
                            if (!(e.target as HTMLElement).classList.contains('video-author') && recommendvideosInfo[i]?.videoID) {
                                handleVideoClick(recommendvideosInfo[i].videoID);
                            }
                        }}>
                            <div className="main-video-cover-container">
                                <img
                                    src={recommendvideosInfo[i]?.cover || DefaultCover}
                                    alt="视频封面"
                                    className="main-video-cover"
                                    onError={(e) => { e.currentTarget.src = DefaultCover; }}
                                />
                            </div>
                            <div className="main-video-info">
                                <div className="main-video-title">{recommendvideosInfo[i]?.title || "暂无视频"}</div>
                                <div className="main-video-meta">
                                    <span
                                        className="main-video-author"
                                        data-user-id={recommendvideosInfo[i]?.uploaderID}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            if (recommendvideosInfo[i]?.uploaderID) {
                                                handleAuthorClick(recommendvideosInfo[i].uploaderID);
                                            }
                                        }}
                                    >
                                        UP主：{recommendvideosInfo[i]?.uploaderInfo?.username || "未知用户"}
                                    </span>
                                    <span className="main-video-time">
                                        {recommendvideosInfo[i]?.uploadTime ? dateformatTime(recommendvideosInfo[i].uploadTime) : ""}
                                    </span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
            
            <div className="main-refresh-container">
                <div 
                    className={`main-refresh-btn ${isRefreshing ? 'spinning' : ''}`}
                    onClick={handleRefresh}
                >
                    <RefreshIcon />
                    <span className="main-refresh-text">换一批</span>
                </div>
            </div>
            </div>
            {showCategoryModal && (
                <div className="main-modal" onClick={() => setShowCategoryModal(false)}>
                    <div className="main-modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="main-modal-header">
                            <div className="main-modal-title">{categoryTitle}</div>
                            <div className="main-modal-close" onClick={() => setShowCategoryModal(false)}>&times;</div>
                        </div>
                        <div className="main-modal-body">
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