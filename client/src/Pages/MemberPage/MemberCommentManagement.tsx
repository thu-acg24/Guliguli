import React, { useState, useEffect, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useUserToken } from "Globals/GlobalStore";
import { QueryVideoCommentsMessage } from "Plugins/CommentService/APIs/QueryVideoCommentsMessage";
import { materialAlertError, materialAlertSuccess } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { memberPagePath } from "./MemberPage";
import { DEFAULT_AVATAR } from "Components/DefaultAvatar";

interface Comment {
    commentID: number;
    content: string;
    user: {
        userID: number;
        username: string;
        avatarPath: string;
    };
    createdAt: string;
    likeCount: number;
    replyCount: number;
    rootID?: number;
    parentID?: number;
}

const MemberCommentManagement: React.FC = () => {
    const { videoID } = useParams<{ videoID: string }>();
    const navigate = useNavigate();
    const userToken = useUserToken();

    const [comments, setComments] = useState<Comment[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [hasMore, setHasMore] = useState(true);
    const [currentPage, setCurrentPage] = useState(0);
    const [selectedRootID, setSelectedRootID] = useState<number | null>(null);
    const [viewingReplies, setViewingReplies] = useState<boolean>(false);

    const pageSize = 20;

    useEffect(() => {
        if (videoID) {
            loadComments(true);
        }
    }, [videoID]);

    const loadComments = useCallback(async (reset: boolean = false) => {
        if (!videoID || (!reset && !hasMore)) return;

        try {
            if (reset) {
                setLoading(true);
                setCurrentPage(0);
                setComments([]);
                setHasMore(true);
            } else {
                setLoadingMore(true);
            }

            const startIndex = reset ? 0 : currentPage * pageSize;
            const endIndex = startIndex + pageSize - 1;

            const response = await new Promise<string>((resolve, reject) => {
                new QueryVideoCommentsMessage(
                    parseInt(videoID),
                    startIndex,
                    endIndex
                ).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const commentsData = JSON.parse(response);

            if (reset) {
                setComments(commentsData);
            } else {
                setComments(prev => [...prev, ...commentsData]);
            }

            setCurrentPage(prev => prev + 1);
            setHasMore(commentsData.length === pageSize);
        } catch (error) {
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取评论列表失败");
        } finally {
            setLoading(false);
            setLoadingMore(false);
        }
    }, [videoID, currentPage, hasMore]);

    const loadReplies = async (rootID: number) => {
        if (!videoID) return;

        try {
            setLoading(true);
            setSelectedRootID(rootID);
            setViewingReplies(true);

            const response = await new Promise<string>((resolve, reject) => {
                new QueryVideoCommentsMessage(
                    parseInt(videoID),
                    0,
                    99 // 加载更多回复
                ).send(
                    (info: string) => resolve(info),
                    (error: string) => reject(new Error(error))
                );
            });

            const repliesData = JSON.parse(response);
            setComments(repliesData);
            setHasMore(false);
        } catch (error) {
            materialAlertError("加载失败", error instanceof Error ? error.message : "获取回复列表失败");
        } finally {
            setLoading(false);
        }
    };

    const handleBackToComments = () => {
        setViewingReplies(false);
        setSelectedRootID(null);
        setComments([]);
        setCurrentPage(0);
        setHasMore(true);
        loadComments(true);
    };

    const handleDeleteComment = async (commentID: number) => {
        if (!userToken) {
            materialAlertError("未登录", "请先登录");
            return;
        }

        if (!confirm("确定要删除这条评论吗？")) {
            return;
        }

        try {
            // 这里应该调用删除评论的API
            // await new DeleteCommentMessage(userToken, commentID).send(...)

            // 暂时模拟删除
            await new Promise(resolve => setTimeout(resolve, 1000));

            setComments(prev => prev.filter(comment => comment.commentID !== commentID));
            materialAlertSuccess("删除成功", "评论已被删除");
        } catch (error) {
            materialAlertError("删除失败", error instanceof Error ? error.message : "删除评论失败");
        }
    };

    const handleViewReplies = (rootID: number) => {
        loadReplies(rootID);
    };

    const formatDate = (dateString: string): string => {
        return new Date(dateString).toLocaleString();
    };

    const handleScroll = useCallback(() => {
        if (loadingMore || !hasMore) return;

        const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
        const scrollHeight = document.documentElement.scrollHeight || document.body.scrollHeight;
        const clientHeight = document.documentElement.clientHeight || window.innerHeight;

        if (scrollTop + clientHeight >= scrollHeight - 100) {
            loadComments(false);
        }
    }, [loadComments, loadingMore, hasMore]);

    useEffect(() => {
        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, [handleScroll]);

    if (loading) {
        return (
            <div className="member-loading">
                <div>加载中...</div>
            </div>
        );
    }

    return (
        <div className="member-comment-management">
            <div className="member-page-header">
                <button
                    className="member-back-btn"
                    onClick={() => viewingReplies ? handleBackToComments() : navigate(memberPagePath)}
                >
                    ← 返回
                </button>
                <h1 className="member-page-title">
                    {viewingReplies ? "管理回复" : "管理评论"}
                </h1>
            </div>

            {comments.length === 0 ? (
                <div className="member-empty-state">
                    <div className="member-empty-icon">💬</div>
                    <div className="member-empty-text">
                        {viewingReplies ? "暂无回复" : "暂无评论"}
                    </div>
                </div>
            ) : (
                <div className="member-comment-list">
                    {comments.map((comment) => (
                        <div key={comment.commentID} className="member-comment-item">
                            <div className="member-comment-header">
                                <img
                                    src={comment.user.avatarPath || DEFAULT_AVATAR}
                                    alt={comment.user.username}
                                    className="member-comment-avatar"
                                />
                                <div className="member-comment-user-info">
                                    <div className="member-comment-author">
                                        {comment.user.username}
                                    </div>
                                    <div className="member-comment-time">
                                        {formatDate(comment.createdAt)}
                                    </div>
                                </div>
                            </div>

                            <div className="member-comment-content">
                                {comment.content}
                            </div>

                            <div className="member-comment-stats">
                                <span>👍 {comment.likeCount}</span>
                                {comment.replyCount > 0 && (
                                    <span>💬 {comment.replyCount} 回复</span>
                                )}
                            </div>

                            <div className="member-comment-actions">
                                {!viewingReplies && comment.replyCount > 0 && (
                                    <button
                                        className="member-comment-reply-btn"
                                        onClick={() => handleViewReplies(comment.commentID)}
                                    >
                                        查看回复
                                    </button>
                                )}
                                <button
                                    className="member-comment-delete-btn"
                                    onClick={() => handleDeleteComment(comment.commentID)}
                                >
                                    删除
                                </button>
                            </div>
                        </div>
                    ))}

                    {loadingMore && (
                        <div className="member-loading-more">
                            <div>加载更多...</div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default MemberCommentManagement;
