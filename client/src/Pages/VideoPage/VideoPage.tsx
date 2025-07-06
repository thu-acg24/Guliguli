import React, { useState, useEffect, useLayoutEffect, useRef } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Header from "Components/Header/Header";
import { useUserInfo } from 'Globals/GlobalStore';
import { useUserToken } from "Globals/GlobalStore";
import LoginModal from "Components/LoginModal/LoginModal";
import { fetchOtherUserInfo } from 'Globals/UserService';
import { Comment } from 'Plugins/CommentService/Objects/Comment';
import { PublishCommentMessage } from 'Plugins/CommentService/APIs/PublishCommentMessage';
import { QueryVideoCommentsMessage } from 'Plugins/CommentService/APIs/QueryVideoCommentsMessage';
import { QueryLikedBatchMessage } from 'Plugins/CommentService/APIs/QueryLikedBatchMessage';
import { UpdateLikeCommentMessage } from 'Plugins/CommentService/APIs/UpdateLikeCommentMessage';
import { DeleteCommentMessage } from 'Plugins/CommentService/APIs/DeleteCommentMessage';
import ReplyModal from 'Components/ReplyModal/ReplyModal';
import { formatTime } from 'Components/GetTime';
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { QueryCommentByIDMessage } from "Plugins/CommentService/APIs/QueryCommentByIDMessage";
import "./VideoPage.css";

export const videoPagePath = "/video/:video_id";

interface CommentWithUserInfo extends Comment {
  userInfo?: UserInfo;
  replies?: CommentWithUserInfo[];
  isLiked?: boolean;
  showAllReplies?: boolean;
  hasMoreReplies?: boolean;
  replyToUser?: UserInfo;
}

const VideoPage: React.FC = () => {
  const { video_id } = useParams<{ video_id: string }>();
  const userToken = useUserToken();
  const navigate = useNavigate();
  const { userInfo } = useUserInfo();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [commentInput, setCommentInput] = useState("");
  const [showBottomCommentBar, setShowBottomCommentBar] = useState(false);
  const [comments, setComments] = useState<CommentWithUserInfo[]>([]);
  const [loadingComments, setLoadingComments] = useState(false);
  const [noMoreComments, setNoMoreComments] = useState(false);
  const [replyingTo, setReplyingTo] = useState<{ id: number, username: string, content: string } | null>(null);
  const [showReplyModal, setShowReplyModal] = useState(false);
  const commentsSectionRef = useRef<HTMLDivElement>(null);
  const commentInputRef = useRef<HTMLDivElement>(null);
  const [isFollowing, setIsFollowing] = useState(false);
  const [isLiked, setIsLiked] = useState(false);
  const [isFavorited, setIsFavorited] = useState(false);

  // Mock data
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
    tags: ["科技", "数码", "评测", "开箱"]
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

  useLayoutEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  useEffect(() => {
    setIsLoggedIn(!!userToken);
    setComments([]);
    setNoMoreComments(false);
    setCommentInput("");
    setReplyingTo(null);
    fetchComments();
  }, [userToken]);

  useEffect(() => {
    const handleScroll = () => {
      if (!commentInputRef.current) return;
      
      const inputRect = commentInputRef.current.getBoundingClientRect();
      const isInputVisible = inputRect.top < window.innerHeight && inputRect.bottom >= 0;
      setShowBottomCommentBar(!isInputVisible);
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const fetchComments = async (lastComment?: CommentWithUserInfo) => {
    if (loadingComments || noMoreComments) return;
    
    setLoadingComments(true);
    try {
      const lastTime = lastComment ? lastComment.timestamp : "9999-12-31 23:59:59";
      const lastID = lastComment?.commentID || 0;
      
      const newComments = await new Promise<Comment[]>((resolve, reject) => {
        new QueryVideoCommentsMessage(
          parseInt(video_id || "0"),
          new Date(lastTime).getTime(),
          lastID,
          null
        ).send(
          (info: string) => {
            try {
              const data: Comment[] = JSON.parse(info);
              resolve(data);
            } catch (e) {
              reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
            }
          },
          (error: string) => reject(new Error(`请求失败: ${error}`))
        );
      });

      if (newComments.length === 0) {
        setNoMoreComments(true);
        return;
      }

      const commentsWithUserInfo = await Promise.all(
        newComments.map(async (comment) => {
          const userInfo = await fetchOtherUserInfo(comment.authorID);
          const commentInstance = comment instanceof Comment ? comment : Object.assign(Object.create(Comment.prototype), comment);
          return Object.assign(commentInstance, {
            userInfo,
            replies: [] as CommentWithUserInfo[],
            showAllReplies: false,
            hasMoreReplies: false,
            isLiked: false
          });
        })
      );

      if (userToken) {
        const likedStatus = await fetchLikedStatus(commentsWithUserInfo.map(c => c.commentID));
        console.log("评论的点赞状态:", likedStatus);
        commentsWithUserInfo.forEach((comment, index) => {
          comment.isLiked = likedStatus[index];
        });
      }

      setComments(prev => [...prev, ...commentsWithUserInfo]);
    } catch (error) {
      console.error('加载评论失败:', error);
    } finally {
      setLoadingComments(false);
    }
  };

  const fetchReplies = async (comment: CommentWithUserInfo) => {
    if (!comment.replyCount) return;
    
    try {
      const newReplies = await new Promise<Comment[]>((resolve, reject) => {
        new QueryVideoCommentsMessage(
          parseInt(video_id || "0"),
          new Date("1970-01-01 00:00:00").getTime(),
          0,
          comment.commentID
        ).send(
          (info: string) => {
            try {
              const data: Comment[] = JSON.parse(info);
              resolve(data);
            } catch (e) {
              reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
            }
          },
          (error: string) => reject(new Error(`请求失败: ${error}`))
        );
      });

      const replyToCommentIDs = newReplies
        .filter(reply => reply.replyToID && reply.replyToID !== comment.commentID)
        .map(reply => reply.replyToID) as number[];
      
      const uniqueReplyToIDs = [...new Set(replyToCommentIDs)];
      const replyToComments = await Promise.all(
        uniqueReplyToIDs.map(async id => {
          try {
            const replyToComment = await new Promise<Comment>((resolve, reject) => {
              new QueryCommentByIDMessage(id).send(
                (info: string) => {
                  try {
                    const data: Comment = JSON.parse(info);
                    resolve(data);
                  } catch (e) {
                    reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
                  }
                },
                (error: string) => reject(new Error(`请求失败: ${error}`))
              );
            });
            return replyToComment;
          } catch (error) {
            console.error(`获取回复目标评论 ${id} 失败:`, error);
            return null;
          }
        })
      );

      const repliesWithUserInfo = await Promise.all(
        newReplies.map(async (reply) => {
          const userInfo = await fetchOtherUserInfo(reply.authorID);
          let replyToUser: UserInfo | undefined;
          
          if (reply.replyToID && reply.replyToID !== comment.commentID) {
            const replyToComment = replyToComments.find(c => c?.commentID === reply.replyToID);
            if (replyToComment) {
              replyToUser = await fetchOtherUserInfo(replyToComment.authorID);
            }
          }
          return { 
            ...reply, 
            userInfo, 
            replyToUser,
            isLiked: userToken ? await fetchLikedStatus([reply.commentID]).then(res => res[0]) : false
          };
        })
      );

      setComments(prev => prev.map(c => {
        if (c.commentID === comment.commentID) {
          return Object.assign(Object.create(Object.getPrototypeOf(c)), {
            ...c,
            replies: repliesWithUserInfo,
            hasMoreReplies: repliesWithUserInfo.length < c.replyCount
          });
        }
        return c;
      }));
    } catch (error) {
      console.error('加载回复失败:', error);
    }
  };

  const fetchLikedStatus = async (commentIDs: number[]): Promise<boolean[]> => {
    if (!userToken || commentIDs.length === 0) return new Array(commentIDs.length).fill(false);
    
    return new Promise((resolve, reject) => {
      new QueryLikedBatchMessage(userToken, commentIDs).send(
        (info: string) => {
          try {
            const data: boolean[] = JSON.parse(info);
            resolve(data);
          } catch (e) {
            reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
          }
        },
        (error: string) => reject(new Error(`请求失败: ${error}`))
      );
    });
  };

  const handleLoadMore = () => {
    if (comments.length === 0 || loadingComments) return;
    fetchComments(comments[comments.length - 1]);
  };

  const handleToggleReplies = async (comment: CommentWithUserInfo) => {
    if (!comment.showAllReplies && (!comment.replies || comment.replies.length === 0)) {
      await fetchReplies(comment);
    }
    
    setComments(prev => prev.map(c => {
      if (c.commentID === comment.commentID) {
        return Object.assign(Object.create(Object.getPrototypeOf(c)), {
          ...c,
          showAllReplies: !c.showAllReplies
        });
      }
      return c;
    }));
  };

  const handleLoadMoreReplies = async (comment: CommentWithUserInfo) => {
    if (!comment.replies || comment.replies.length === 0) return;
    
    try {
      const lastReply = comment.replies[comment.replies.length - 1];
      const newReplies = await new Promise<Comment[]>((resolve, reject) => {
        new QueryVideoCommentsMessage(
          parseInt(video_id || "0"),
          new Date(lastReply.timestamp).getTime(),
          lastReply.commentID,
          comment.commentID
        ).send(
          (info: string) => {
            try {
              const data: Comment[] = JSON.parse(info);
              resolve(data);
            } catch (e) {
              reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
            }
          },
          (error: string) => reject(new Error(`请求失败: ${error}`))
        );
      });

      if (newReplies.length === 0) {
        setComments(prev => prev.map(c => {
          if (c.commentID === comment.commentID) {
            return Object.assign(Object.create(Object.getPrototypeOf(c)), {
              ...c,
              hasMoreReplies: false
            });
          }
          return c;
        }));
        return;
      }

      const replyToCommentIDs = newReplies
        .filter(reply => reply.replyToID && reply.replyToID !== comment.commentID)
        .map(reply => reply.replyToID) as number[];
      
      const uniqueReplyToIDs = [...new Set(replyToCommentIDs)];
      const replyToComments = await Promise.all(
        uniqueReplyToIDs.map(async id => {
          try {
            const replyToComment = await new Promise<Comment>((resolve, reject) => {
              new QueryCommentByIDMessage(id).send(
                (info: string) => {
                  try {
                    const data: Comment = JSON.parse(info);
                    resolve(data);
                  } catch (e) {
                    reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
                  }
                },
                (error: string) => reject(new Error(`请求失败: ${error}`))
              );
            });
            return replyToComment;
          } catch (error) {
            console.error(`获取回复目标评论 ${id} 失败:`, error);
            return null;
          }
        })
      );

      const repliesWithUserInfo = await Promise.all(
        newReplies.map(async (reply) => {
          const userInfo = await fetchOtherUserInfo(reply.authorID);
          let replyToUser: UserInfo | undefined;
          
          if (reply.replyToID && reply.replyToID !== comment.commentID) {
            const replyToComment = replyToComments.find(c => c?.commentID === reply.replyToID);
            if (replyToComment) {
              replyToUser = await fetchOtherUserInfo(replyToComment.authorID);
            }
          }
          
          return { 
            ...reply, 
            userInfo, 
            replyToUser,
            isLiked: userToken ? await fetchLikedStatus([reply.commentID]).then(res => res[0]) : false
          };
        })
      );

      setComments(prev => prev.map(c => {
        if (c.commentID === comment.commentID) {
          return Object.assign(Object.create(Object.getPrototypeOf(c)), {
            ...c,
            replies: [...(c.replies || []), ...repliesWithUserInfo],
            hasMoreReplies: (c.replies?.length || 0) + newReplies.length < c.replyCount
          });
        }
        return c;
      }));
    } catch (error) {
      console.error('加载更多回复失败:', error);
    }
  };

  const handleLikeComment = async (commentID: number) => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }

    try {
      const comment = comments.find(c => c.commentID === commentID) || 
                     comments.flatMap(c => c.replies || []).find(r => r.commentID === commentID);
      
      if (!comment) return;
      console.log("当前评论" ,comment);
      const isLike = !comment.isLiked;
      await new Promise((resolve, reject) => {
        new UpdateLikeCommentMessage(userToken, commentID, isLike).send(
          () => resolve(true),
          (error) => reject(new Error(`请求失败: ${error}`))
        );
      });

      setComments(prev => prev.map(c => {
        if (c.commentID === commentID) {
          return Object.assign(Object.create(Object.getPrototypeOf(c)), {
            ...c,
            isLiked: isLike,
            likes: isLike ? c.likes + 1 : c.likes - 1
          });
        }

        if (c.replies) {
          return Object.assign(Object.create(Object.getPrototypeOf(c)), {
            ...c,
            replies: c.replies.map(r => {
              if (r.commentID === commentID) {
                return Object.assign(Object.create(Object.getPrototypeOf(r)), {
                  ...r,
                  isLiked: isLike,
                  likes: isLike ? r.likes + 1 : r.likes - 1
                });
              }
              return r;
            })
          });
        }

        return c;
      }));
    } catch (error) {
      console.error('点赞失败:', error);
    }
  };

  const handleDeleteComment = async (commentID: number) => {
    if (!isLoggedIn) return;
    
    try {
      await new Promise((resolve, reject) => {
        new DeleteCommentMessage(userToken, commentID).send(
          () => resolve(true),
          (error) => reject(new Error(`请求失败: ${error}`))
        );
      });

      setComments(prev => {
        const commentIndex = prev.findIndex(c => c.commentID === commentID);
        if (commentIndex !== -1) {
          return [
            ...prev.slice(0, commentIndex),
            ...prev.slice(commentIndex + 1)
          ];
        }

        return prev.map(c => {
          if (c.replies) {
            return Object.assign(Object.create(Object.getPrototypeOf(c)), {
              ...c,
              replies: c.replies.filter(r => r.commentID !== commentID),
              replyCount: c.replies.some(r => r.commentID === commentID) ? c.replyCount - 1 : c.replyCount
            });
          }
          return c;
        });
      });
    } catch (error) {
      console.error('删除评论失败:', error);
    }
  };

  const handlePostComment = async () => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }
    
    if (!commentInput.trim()) return;

    try {
      const newComment = await new Promise<Comment>((resolve, reject) => {
        new PublishCommentMessage(
          userToken,
          parseInt(video_id),
          commentInput,
          null
        ).send(
          (info: string) => {
            try {
              const data: Comment = JSON.parse(info);
              resolve(data);
            } catch (e) {
              reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
            }
          },
          (error) => reject(new Error(`请求失败: ${error}`))
        );
      });

      const userInfo = await fetchOtherUserInfo(newComment.authorID);
      setComments(prev => [
        Object.assign(Object.create(Object.getPrototypeOf(newComment)), {
          ...newComment,
          userInfo,
          isLiked: false,
          replies: [],
          showAllReplies: false,
          hasMoreReplies: false
        }),
        ...prev
      ]);

      setCommentInput("");
    } catch (error) {
      console.error('发布评论失败:', error);
    }
  };


  const navigateToUser = (userID: number) => {
    navigate(`/home/${userID}`);
  };

  const followUp = (upID: string) => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }
    setIsFollowing(!isFollowing);
  };

  const likeVideo = (videoID: string) => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }
    setIsLiked(!isLiked);
  };

  const favoriteVideo = (videoID: string) => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }
    setIsFavorited(!isFavorited);
  };

  return (
    <div className="video-video-page">
      <Header />

      <div className="video-video-page-container">
        {/* Main content area */}
        <div className="video-video-main-content">
          {/* Video player section */}
          <div className="video-video-player-section">
            <h1 className="video-video-title">{videoData.title}</h1>

            <div className="video-video-meta">
              <span>播放: {videoData.views}</span>
              <span>弹幕: {videoData.danmaku}</span>
              <span>投稿时间: {videoData.uploadDate}</span>
            </div>

            <div className="video-video-player-container">
              <img
                src="https://picsum.photos/800/450?random=100"
                alt="视频播放器"
                className="video-video-player"
                onError={(e) => { e.currentTarget.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIzMDAiIGhlaWdodD0iMTY5IiB2aWV3Qm94PSIwIDAgMzAwIDE2OSIgZmlsbD0iI2ZmY2NkNSI+PHJlY3Qgd2lkdGg9IjMwMCIgaGVpZ2h0PSIxNjkiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE0IiBmaWxsPSIjZmY3Mjk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIj7lm77niYflnLDor50v5paH5a2XPC90ZXh0Pjwvc3ZnPg=='; }}
              />
            </div>

            <div className="video-video-actions">
              <button
                className={`video-videopage-action-btn ${isLiked ? 'liked' : ''}`}
                onClick={() => likeVideo(videoData.id)}
              >
                <span className="video-icon">👍</span> {isLiked ? '已点赞' : '点赞'}
              </button>
              <button
                className={`video-videopage-action-btn ${isFavorited ? 'favorited' : ''}`}
                onClick={() => favoriteVideo(videoData.id)}
              >
                <span className="video-icon">⭐</span> {isFavorited ? '已收藏' : '收藏'}
              </button>
            </div>

            <div className="video-video-tags">
              {videoData.tags.map(tag => (
                <span
                  key={tag}
                  className="video-tag"
                  onClick={() => alert(`搜索标签: ${tag}`)}
                >
                  {tag}
                </span>
              ))}
            </div>
          </div>

          {/* Comments section */}
          <div className="video-comments-container" ref={commentsSectionRef}>
            <div className="video-comments-header">
              <h3>评论 ({comments.reduce((acc, c) => acc + 1 + (c.replies?.length || 0), 0)})</h3>
            </div>

            {/* Comment input */}
            <div className="video-comment-input-area" ref={commentInputRef}>
              <img
                src={isLoggedIn ? (userInfo?.avatarPath || '/default-avatar.png') : '/default-avatar.png'}
                alt="用户头像"
                className="video-comment-avatar"
                onClick={() => isLoggedIn && navigateToUser(userInfo?.userID || 0)}
              />
              <div className="video-comment-input-wrapper">
                <input
                  type="text"
                  placeholder="发一条友善的评论"
                  value={commentInput}
                  onChange={(e) => setCommentInput(e.target.value)}
                  onKeyPress={(e) => e.key === 'Enter' && handlePostComment()}
                />
              </div>
            </div>

            {/* Comments list */}
            <div className="video-comments-list">
              {comments.map(comment => (
                <div key={comment.commentID} className="video-comment-item">
                  <div className="video-comment-main">
                    <img
                      src={comment.userInfo?.avatarPath || '/default-avatar.png'}
                      alt="用户头像"
                      className="video-comment-avatar"
                      onClick={() => navigateToUser(comment.authorID)}
                    />
                    <div className="video-comment-content">
                      <div className="video-comment-header">
                        <span 
                          className="video-comment-username"
                          onClick={() => navigateToUser(comment.authorID)}
                        >
                          {comment.userInfo?.username || '未知用户'}
                        </span>
                        <span className="video-comment-time">{formatTime(comment.timestamp)}</span>
                      </div>
                      <div className="video-comment-text">{comment.content}</div>
                      <div className="video-comment-actions">
                        <button
                          className={`video-like-btn ${comment.isLiked ? 'liked' : ''}`}
                          onClick={() => handleLikeComment(comment.commentID)}
                        >
                          <span>👍</span> {comment.likes}
                        </button>
                        <button
                          className="video-reply-btn"
                          onClick={() => {
            
                            if (!isLoggedIn) {
                              setShowLoginModal(true);
                              return;
                            }
                            setReplyingTo({ 
                              id: comment.commentID, 
                              username: comment.userInfo?.username || '用户',
                              content: comment.content
                            });
                            setShowReplyModal(true);
                          }}
                        >
                          回复
                        </button>
                        {comment.authorID === userInfo?.userID && (
                          <button
                            className="video-delete-btn"
                            onClick={() => handleDeleteComment(comment.commentID)}
                          >
                            删除
                          </button>
                        )}
                      </div>

                      {/* Replies section */}
                      {comment.replyCount > 0 && (
                        <div className="video-replies-section">
                          {!comment.showAllReplies && comment.replies && comment.replies.length > 0 && (
                            <div className="video-replies-list">
                              {comment.replies.slice(0, 2).map(reply => (
                                <div key={reply.commentID} className="video-reply-item">
                                  <img
                                    src={reply.userInfo?.avatarPath || '/default-avatar.png'}
                                    alt="用户头像"
                                    className="video-reply-avatar"
                                    onClick={() => navigateToUser(reply.authorID)}
                                  />
                                  <div className="video-reply-content">
                                    <div className="video-reply-header">
                                      <span 
                                        className="video-reply-username"
                                        onClick={() => navigateToUser(reply.authorID)}
                                      >
                                        {reply.userInfo?.username || '未知用户'}
                                      </span>
                                      <span className="video-reply-time">{formatTime(reply.timestamp)}</span>
                                    </div>
                                    <div className="video-reply-text">
                                      {reply.replyToUser && (
                                        <>
                                          回复&nbsp;
                                          <span 
                                            className="video-reply-highlight"
                                            onClick={() => navigateToUser(reply.replyToUser?.userID || 0)}
                                          >
                                            @{reply.replyToUser.username}：
                                          </span>
                                        </>
                                      )}
                                      {reply.content}
                                    </div>
                                    <div className="video-reply-actions">
                                      <button
                                        className={`video-like-btn ${reply.isLiked ? 'liked' : ''}`}
                                        onClick={() => handleLikeComment(reply.commentID)}
                                      >
                                        <span>👍</span> {reply.likes}
                                      </button>
                                      <button
                                        className="video-reply-btn"
                                        onClick={() => {
                                          setReplyingTo({ 
                                            id: reply.commentID, 
                                            username: reply.userInfo?.username || '用户',
                                            content: reply.content
                                          });
                                          setShowReplyModal(true);
                                        }}
                                      >
                                        回复
                                      </button>
                                      {reply.authorID === userInfo?.userID && (
                                        <button
                                          className="video-delete-btn"
                                          onClick={() => handleDeleteComment(reply.commentID)}
                                        >
                                          删除
                                        </button>
                                      )}
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}

                          {comment.showAllReplies && comment.replies && comment.replies.length > 0 && (
                            <div className="video-replies-list">
                              {comment.replies.map(reply => (
                                <div key={reply.commentID} className="video-reply-item">
                                  <img
                                    src={reply.userInfo?.avatarPath || '/default-avatar.png'}
                                    alt="用户头像"
                                    className="video-reply-avatar"
                                    onClick={() => navigateToUser(reply.authorID)}
                                  />
                                  <div className="video-reply-content">
                                    <div className="video-reply-header">
                                      <span 
                                        className="video-reply-username"
                                        onClick={() => navigateToUser(reply.authorID)}
                                      >
                                        {reply.userInfo?.username || '未知用户'}
                                      </span>
                                      <span className="video-reply-time">{formatTime(reply.timestamp)}</span>
                                    </div>
                                    <div className="video-reply-text">
                                      {reply.replyToUser && (
                                        <>
                                          回复&nbsp;
                                          <span 
                                            className="video-reply-highlight"
                                            onClick={() => navigateToUser(reply.replyToUser?.userID || 0)}
                                          >
                                            @{reply.replyToUser.username}：
                                          </span>
                                        </>
                                      )}
                                      {reply.content}
                                    </div>
                                    <div className="video-reply-actions">
                                      <button
                                        className={`video-like-btn ${reply.isLiked ? 'liked' : ''}`}
                                        onClick={() => handleLikeComment(reply.commentID)}
                                      >
                                        <span>👍</span> {reply.likes}
                                      </button>
                                      <button
                                        className="video-reply-btn"
                                        onClick={() => {
                                          
                                          if (!isLoggedIn) {
                                            setShowLoginModal(true);
                                            return;
                                          }
                                          setReplyingTo({ 
                                            id: reply.commentID, 
                                            username: reply.userInfo?.username || '用户',
                                            content: reply.content
                                          });
                                          setShowReplyModal(true);
                                        }}
                                      >
                                        回复
                                      </button>
                                      {reply.authorID === userInfo?.userID && (
                                        <button
                                          className="video-delete-btn"
                                          onClick={() => handleDeleteComment(reply.commentID)}
                                        >
                                          删除
                                        </button>
                                      )}
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}

                          <div className="video-reply-actions">
                            {comment.replyCount > 0 && (
                              <span 
                                className="video-view-replies" 
                                onClick={() => handleToggleReplies(comment)}
                              >
                                {comment.showAllReplies ? '收起' : `共${comment.replyCount}条回复，点击查看`}
                              </span>
                            )}
                            {comment.showAllReplies && comment.hasMoreReplies && (
                              <span 
                                className="video-load-more-replies" 
                                onClick={() => handleLoadMoreReplies(comment)}
                              >
                                点击查看更多回复
                              </span>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))}

              {loadingComments && (
                <div className="video-comments-loading">加载中...</div>
              )}
              
              {noMoreComments && comments.length > 0 && (
                <div className="video-comments-end">没有更多评论了</div>
              )}
              
              {!noMoreComments && comments.length > 0 && (
                <div className="video-load-more" onClick={handleLoadMore}>
                  点击加载更多评论
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <div className="video-video-sidebar">
          {/* UP info */}
          <div className="video-up-info">
            <div className="video-up-avatar">
              <img src={videoData.author.avatar} alt="UP主头像" />
            </div>
            <div className="video-up-details">
              <div className="video-up-name">{videoData.author.name}</div>
              <div className="video-up-description" title={videoData.author.description}>
                {videoData.author.description.length > 20
                  ? `${videoData.author.description.substring(0, 20)}...`
                  : videoData.author.description}
              </div>
              <button
                className={`video-follow-btn ${isFollowing ? 'following' : ''}`}
                onClick={() => followUp(videoData.author.id)}
              >
                {isFollowing ? '已关注' : '关注'}
              </button>
            </div>
          </div>

          {/* Recommended videos */}
          <div className="video-recommended-videos">
            <h3 className="video-recommended-title">推荐视频</h3>
            {recommendedVideos.map(video => (
              <div key={video.id} className="video-recommended-video">
                <div
                  className="video-recommended-cover"
                  onClick={() => alert(`跳转到视频 ${video.id}`)}
                >
                  <img src={video.cover} alt="视频封面" />
                </div>
                <div className="video-recommended-info">
                  <div
                    className="video-recommended-title"
                    onClick={() => alert(`跳转到视频 ${video.id}`)}
                  >
                    {video.title}
                  </div>
                  <div
                    className="video-recommended-author"
                    onClick={() => alert(`跳转到UP主 ${video.author}`)}
                  >
                    {video.author}
                  </div>
                  <div className="video-recommended-meta">
                    <span>播放: {video.views}</span>
                    <span>弹幕: {video.danmaku}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Bottom comment bar */}
      {showBottomCommentBar && (
        <div className="video-bottom-comment-bar">
          <input
            type="text"
            placeholder="发一条友善的评论"
            value={commentInput}
            onChange={(e) => setCommentInput(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handlePostComment()}
          />
          <button onClick={handlePostComment}>发送</button>
        </div>
      )}

      {/* Login Modal */}
      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
      />

      {/* Reply Modal */}
      
      {showReplyModal && replyingTo && (
        <ReplyModal
            videoID={parseInt(video_id || "0")}
            commentID={replyingTo.id}
            replyingToContent={replyingTo.content}
            content={commentInput}
            onClose={() => {setCommentInput(""), setShowReplyModal(false), setReplyingTo(null)}}
            onSuccess={() => {setCommentInput(""), setShowReplyModal(false), setReplyingTo(null)}}
        />
      )}
    </div>
  );
};

export default VideoPage;