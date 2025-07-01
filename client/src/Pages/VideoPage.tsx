// VideoPage.tsx
import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import Header from "../Components/Header";
import LoginModal from "../Components/LoginModal";

export const videoPagePath = "/video/:id";

const VideoPage: React.FC = () => {
    const navigate = useNavigate();
    const [isLoggedIn, setIsLoggedIn] = useState(false); // Assume not logged in by default
    const [showLoginModal, setShowLoginModal] = useState(false);
    const [commentInput, setCommentInput] = useState("");
    const [replyInput, setReplyInput] = useState("");
    const [replyingTo, setReplyingTo] = useState<{id: string, username: string} | null>(null);
    const [showReplyModal, setShowReplyModal] = useState(false);
    const [isFollowing, setIsFollowing] = useState(false);
    const [isLiked, setIsLiked] = useState(false);
    const [isFavorited, setIsFavorited] = useState(false);

    // Mock data - these would come from API calls
    const [videoData, setVideoData] = useState({
        id: "123",
        title: "这是一个视频标题，可能会比较长，需要显示两行",
        views: "123.4万",
        danmaku: "2.3万",
        uploadDate: "2023-10-15",
        author: {
            id: "456",
            name: "UP主名称",
            avatar: "https://picsum.photos/50/50?random=1",
            description: "这是一个UP主的简介，可能会比较长，需要显示省略号..."
        },
        tags: ["科技", "数码", "评测", "开箱"],
        comments: [
            {
                id: "c1",
                user: {
                    id: "u1",
                    name: "用户1",
                    avatar: "https://picsum.photos/50/50?random=2"
                },
                content: "这是一条一级评论内容",
                time: "2023-10-16 12:30",
                likes: 123,
                isLiked: false,
                replies: [
                    {
                        id: "r1",
                        user: {
                            id: "u2",
                            name: "用户2",
                            avatar: "https://picsum.photos/50/50?random=3"
                        },
                        content: "这是一条回复评论内容",
                        time: "2023-10-16 13:45",
                        likes: 45,
                        isLiked: false,
                        replyTo: "用户1"
                    }
                ]
            },
            {
                id: "c2",
                user: {
                    id: "u3",
                    name: "用户3",
                    avatar: "https://picsum.photos/50/50?random=4"
                },
                content: "这是另一条一级评论内容",
                time: "2023-10-16 14:20",
                likes: 67,
                isLiked: false,
                replies: []
            }
        ]
    });

    const [recommendedVideos, setRecommendedVideos] = useState([
        {
            id: "101",
            title: "推荐视频1",
            author: "UP主1",
            views: "10.2万",
            danmaku: "1.1万",
            cover: "https://picsum.photos/160/90?random=5"
        },
        {
            id: "102",
            title: "推荐视频2",
            author: "UP主2",
            views: "8.7万",
            danmaku: "0.9万",
            cover: "https://picsum.photos/160/90?random=6"
        },
        {
            id: "103",
            title: "推荐视频3",
            author: "UP主3",
            views: "15.3万",
            danmaku: "2.4万",
            cover: "https://picsum.photos/160/90?random=7"
        },
        {
            id: "104",
            title: "推荐视频4",
            author: "UP主4",
            views: "5.6万",
            danmaku: "0.5万",
            cover: "https://picsum.photos/160/90?random=8"
        }
    ]);

    // API placeholder functions
    const followUp = (upId: string) => {
        if (!isLoggedIn) {
            setShowLoginModal(true);
            return;
        }
        // API call would go here
        // Example: fetch(`/api/follow/${upId}`, { method: 'POST' })
        setIsFollowing(!isFollowing);
    };

    const likeVideo = (videoId: string) => {
        if (!isLoggedIn) {
            setShowLoginModal(true);
            return;
        }
        // API call would go here
        // Example: fetch(`/api/like/${videoId}`, { method: 'POST' })
        setIsLiked(!isLiked);
    };

    const favoriteVideo = (videoId: string) => {
        if (!isLoggedIn) {
            setShowLoginModal(true);
            return;
        }
        // API call would go here
        // Example: fetch(`/api/favorite/${videoId}`, { method: 'POST' })
        setIsFavorited(!isFavorited);
    };

    const postComment = () => {
        if (!isLoggedIn) {
            setShowLoginModal(true);
            return;
        }
        if (!commentInput.trim()) return;
        
        // API call would go here
        // Example: fetch('/api/comment', { 
        //   method: 'POST', 
        //   body: JSON.stringify({ videoId: videoData.id, content: commentInput }) 
        // })
        alert(`发布评论: ${commentInput}`);
        setCommentInput("");
    };

    const postReply = () => {
        if (!isLoggedIn) {
            setShowLoginModal(true);
            return;
        }
        if (!replyInput.trim()) return;
        
        // API call would go here
        // Example: fetch('/api/reply', { 
        //   method: 'POST', 
        //   body: JSON.stringify({ 
        //     commentId: replyingTo?.id, 
        //     content: replyInput,
        //     replyToUserId: replyingTo?.id 
        //   }) 
        // })
        alert(`回复 ${replyingTo?.username}: ${replyInput}`);
        setReplyInput("");
        setReplyingTo(null);
        setShowReplyModal(false);
    };

    const likeComment = (commentId: string) => {
        if (!isLoggedIn) {
            setShowLoginModal(true);
            return;
        }
        // API call would go here
        // Example: fetch(`/api/comment/${commentId}/like`, { method: 'POST' })
        alert(`点赞评论 ${commentId}`);
    };

    return (
        <div className="video-page">
            <Header />
            
            <div className="video-page-container">
                {/* Main content area */}
                <div className="video-main-content">
                    {/* Video player section */}
                    <div className="video-player-section">
                        <h1 className="video-title">{videoData.title}</h1>
                        
                        <div className="video-meta">
                            <span>播放: {videoData.views}</span>
                            <span>弹幕: {videoData.danmaku}</span>
                            <span>投稿时间: {videoData.uploadDate}</span>
                        </div>
                        
                        <div className="video-player-container">
                            <img 
                                src="https://picsum.photos/800/450?random=100" 
                                alt="视频播放器" 
                                className="video-player"
                                onError={(e) => { e.currentTarget.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMTY5IiB2aWV3Qm94PSIwIDAgMzAwIDE2OSIgZmlsbD0iI2ZmY2NkNSI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIxNjkiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE0IiBmaWxsPSIjZmY3Mjk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIj7lm77niYflnLDor50v5paH5a2XPC90ZXh0Pjwvc3ZnPg=='; }}
                            />
                        </div>
                        
                        <div className="video-actions">
                            <button 
                                className={`videopage-action-btn ${isLiked ? 'liked' : ''}`}
                                onClick={() => likeVideo(videoData.id)}
                            >
                                <span className="icon">👍</span> {isLiked ? '已点赞' : '点赞'}
                            </button>
                            <button 
                                className={`videopage-action-btn ${isFavorited ? 'favorited' : ''}`}
                                onClick={() => favoriteVideo(videoData.id)}
                            >
                                <span className="icon">⭐</span> {isFavorited ? '已收藏' : '收藏'}
                            </button>
                        </div>
                        
                        <div className="video-tags">
                            {videoData.tags.map(tag => (
                                <span 
                                    key={tag} 
                                    className="tag"
                                    onClick={() => alert(`搜索标签: ${tag}`)}
                                >
                                    {tag}
                                </span>
                            ))}
                        </div>
                    </div>
                    
                    {/* Comments section */}
                    <div className="comments-section">
                        <h3 className="comments-title">评论 ({videoData.comments.length})</h3>
                        
                        {/* Comment input */}
                        <div className="comment-input-container">
                            <div className="user-avatar">
                                <img 
                                    src={isLoggedIn ? "https://picsum.photos/50/50?random=9" : "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWRlLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI3IiByPSI0Ii8+PC9zdmc+"} 
                                    alt="用户头像" 
                                />
                            </div>
                            <input
                                type="text"
                                className="comment-input"
                                placeholder="发一条友善的评论"
                                value={commentInput}
                                onChange={(e) => setCommentInput(e.target.value)}
                                onKeyPress={(e) => e.key === 'Enter' && postComment()}
                            />
                        </div>
                        
                        {/* Comments list */}
                        <div className="comments-list">
                            {videoData.comments.map(comment => (
                                <div key={comment.id} className="comment-item">
                                    <div className="comment-user-avatar">
                                        <img src={comment.user.avatar} alt="用户头像" />
                                    </div>
                                    <div className="comment-content">
                                        <div className="comment-user-name">{comment.user.name}</div>
                                        <div className="comment-text">{comment.content}</div>
                                        <div className="comment-meta">
                                            <span className="comment-time">{comment.time}</span>
                                            <span className="comment-likes">{comment.likes}</span>
                                            <button 
                                                className="like-btn"
                                                onClick={() => likeComment(comment.id)}
                                            >
                                                👍
                                            </button>
                                            <button 
                                                className="reply-btn"
                                                onClick={() => {
                                                    setReplyingTo({id: comment.id, username: comment.user.name});
                                                    setShowReplyModal(true);
                                                }}
                                            >
                                                回复
                                            </button>
                                        </div>
                                        
                                        {/* Replies */}
                                        {comment.replies.length > 0 && (
                                            <div className="replies-list">
                                                {comment.replies.map(reply => (
                                                    <div key={reply.id} className="reply-item">
                                                        <div className="reply-user-avatar">
                                                            <img src={reply.user.avatar} alt="用户头像" />
                                                        </div>
                                                        <div className="reply-content">
                                                            <div className="reply-user-name">
                                                                {reply.user.name} <span className="reply-to">@{reply.replyTo}</span>
                                                            </div>
                                                            <div className="reply-text">{reply.content}</div>
                                                            <div className="reply-meta">
                                                                <span className="reply-time">{reply.time}</span>
                                                                <span className="reply-likes">{reply.likes}</span>
                                                                <button 
                                                                    className="like-btn"
                                                                    onClick={() => likeComment(reply.id)}
                                                                >
                                                                    👍
                                                                </button>
                                                                <button 
                                                                    className="reply-btn"
                                                                    onClick={() => {
                                                                        setReplyingTo({id: comment.id, username: reply.user.name});
                                                                        setShowReplyModal(true);
                                                                    }}
                                                                >
                                                                    回复
                                                                </button>
                                                            </div>
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
                
                {/* Sidebar */}
                <div className="video-sidebar">
                    {/* UP info */}
                    <div className="up-info">
                        <div className="up-avatar">
                            <img src={videoData.author.avatar} alt="UP主头像" />
                        </div>
                        <div className="up-details">
                            <div className="up-name">{videoData.author.name}</div>
                            <div className="up-description" title={videoData.author.description}>
                                {videoData.author.description.length > 20 
                                    ? `${videoData.author.description.substring(0, 20)}...` 
                                    : videoData.author.description}
                            </div>
                            <button 
                                className={`follow-btn ${isFollowing ? 'following' : ''}`}
                                onClick={() => followUp(videoData.author.id)}
                            >
                                {isFollowing ? '已关注' : '关注'}
                            </button>
                        </div>
                    </div>
                    
                    {/* Recommended videos */}
                    <div className="recommended-videos">
                        <h3 className="recommended-title">推荐视频</h3>
                        {recommendedVideos.map(video => (
                            <div key={video.id} className="recommended-video">
                                <div 
                                    className="recommended-cover"
                                    onClick={() => alert(`跳转到视频 ${video.id}`)}
                                >
                                    <img src={video.cover} alt="视频封面" />
                                </div>
                                <div className="recommended-info">
                                    <div 
                                        className="recommended-title"
                                        onClick={() => alert(`跳转到视频 ${video.id}`)}
                                    >
                                        {video.title}
                                    </div>
                                    <div 
                                        className="recommended-author"
                                        onClick={() => alert(`跳转到UP主 ${video.author}`)}
                                    >
                                        {video.author}
                                    </div>
                                    <div className="recommended-meta">
                                        <span>播放: {video.views}</span>
                                        <span>弹幕: {video.danmaku}</span>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
            
            {/* Login Modal */}
            <LoginModal 
                isOpen={showLoginModal} 
                onClose={() => setShowLoginModal(false)} 
            />
            
            {/* Reply Modal */}
            {showReplyModal && (
                <div className="modal" onClick={() => setShowReplyModal(false)}>
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <div className="modal-header">
                            <div className="modal-title">回复 @{replyingTo?.username}</div>
                            <div className="modal-close" onClick={() => setShowReplyModal(false)}>&times;</div>
                        </div>
                        <div className="modal-body">
                            <input
                                type="text"
                                className="form-input"
                                placeholder={`回复 @${replyingTo?.username}`}
                                value={replyInput}
                                onChange={(e) => setReplyInput(e.target.value)}
                                onKeyPress={(e) => e.key === 'Enter' && postReply()}
                                autoFocus
                            />
                            <button 
                                className="login-btn"
                                onClick={postReply}
                            >
                                回复
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default VideoPage;