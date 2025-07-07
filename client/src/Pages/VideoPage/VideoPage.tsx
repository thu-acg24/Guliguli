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
import { Video } from 'Plugins/VideoService/Objects/Video';
import { QueryVideoInfoMessage } from 'Plugins/VideoService/APIs/QueryVideoInfoMessage';
import { QueryFavoriteMessage } from 'Plugins/VideoService/APIs/QueryFavoriteMessage';
import { QueryLikeMessage } from 'Plugins/VideoService/APIs/QueryLikeMessage';
import { ChangeLikeMessage } from 'Plugins/VideoService/APIs/ChangeLikeMessage';
import { ChangeFavoriteMessage } from 'Plugins/VideoService/APIs/ChangeFavoriteMessage'
import {UserStat} from 'Plugins/UserService/Objects/UserStat'
import {QueryUserStatMessage} from 'Plugins/UserService/APIs/QueryUserStatMessage'
import {QueryFollowMessage} from 'Plugins/UserService/APIs/QueryFollowMessage'
import {ChangeFollowStatusMessage} from 'Plugins/UserService/APIs/ChangeFollowStatusMessage'
import HlsVideoPlayerWrapper from "./HlsVideoPlayerWrapper";
import "./VideoPage.css";
import { set } from "lodash";

export const videoPagePath = "/video/:video_id";

interface CommentWithUserInfo extends Comment {
  isLocal?:boolean;
  userInfo?: UserInfo;
  replies?: CommentWithUserInfo[];
  isLiked?: boolean;
  showAllReplies?: boolean;
  hasMoreReplies?: boolean;
  replyToUsername?: string;
}
const VideoPage: React.FC = () => {
  const { video_id } = useParams<{ video_id: string }>();
  const userToken = useUserToken();
  const navigate = useNavigate();
  const { userInfo } = useUserInfo();
  const [uploaderInfo, setUploaderInfo] = useState<UserInfo | null>(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [videoinfoisloading, setVideoinfoIsLoading] = useState(true);
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
  const [likeisprocessing, setLikeisprocessing] = useState(false);
  const [followisprocessing, setFollowisprocessing] = useState(false);
  const [favoriteisprocessing, setFavoriteisprocessing] = useState(false);
  const [videoinfo, setVideoinfo] =  useState<Video>(null);
  const [upstat, setUpstat] = useState<UserStat>();
  
  const getVideoUrl = () => {
      return `http://183.173.211.15:5004/browser/video-server/testid%2Fvideo%2Findex.m3u8`;
    };
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
    console.log("现在正在看的是", video_id);
    setVideoinfoIsLoading(true);
    window.scrollTo(0, 0);
    fetchVideoInfo()
  }, [video_id]);
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
      const isInputVisible = inputRect.bottom >= 0;
      setShowBottomCommentBar(!isInputVisible);
    };

    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);
  useEffect(() => {
    
    console.log("开始获取用户点赞收藏关注");
    if(!userToken || !videoinfo) return;
    console.log("当前视频信息",videoinfo);
    new QueryLikeMessage(userToken, Number(video_id)).send(
      (info: string) => {
        const isLiked = JSON.parse(info);
        setIsLiked(isLiked);
      },
      (error: string) => {
        console.error("获取用户点赞状态失败:", error);
      }
    );

    new QueryFavoriteMessage(userToken, Number(video_id)).send(
      (info: string) => {
        const isFavorited = JSON.parse(info);
        setIsFavorited(isFavorited);
      },
      (error: string) => {
        console.error("获取用户收藏状态失败:", error);
      }
    );
    if(userInfo.userID===videoinfo.uploaderID)return;
    new QueryFollowMessage(userInfo.userID, videoinfo.uploaderID).send(
      (info: string) => {
        const isFollowing = JSON.parse(info);
        setIsFollowing(isFollowing);
      },
      (error: string) => {
        console.error("获取用户关注状态失败:", error);
      }
    );
  }, [userInfo,videoinfo]);
  const fetchVideoInfo = async () => {
    if (!video_id) return;
    try { 
      const videoInfo = await new Promise<Video>((resolve, reject) => {
        new QueryVideoInfoMessage(null, Number(video_id)).send(
          (info: string) => {
            try {
              const data: Video = JSON.parse(info);
              resolve(data);
            } catch (e) {
              reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
            }
          },
          (error: string) => reject(new Error(`请求失败: ${error}`))
        );
      });
      
      const upStat=await new Promise<UserStat>((resolve, reject) => {
        new QueryUserStatMessage(videoInfo.uploaderID).send(
          (info: string) => {
            try {
              const data: UserStat = JSON.parse(info);
              resolve(data);
            } catch (e) {
              reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
            }
          },
          (error: string) => reject(new Error(`请求失败: ${error}`))
        );
      });
      setVideoinfo(videoInfo);
      setUpstat(upStat);
      const uploaderInfo = await fetchOtherUserInfo(videoInfo.uploaderID);
      setUploaderInfo(uploaderInfo);
    } catch (error) {
      console.error('加载视频信息失败:', error);
    }finally {
      setVideoinfoIsLoading(false);
    }
  }
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
      
      const limit=Math.min(10,comment.replyCount-(comment.replies?.length||0));
      if(limit<=0)return;
      const newReplies = await new Promise<Comment[]>((resolve, reject) => {
        new QueryVideoCommentsMessage(
          parseInt(video_id || "0"),
          new Date("1970-01-01 00:00:00").getTime(),
          0,
          comment.commentID,
          limit
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
      console.log("获取的回复",newReplies);
      const repliesWithUserInfo = await Promise.all(
        newReplies.map(async (reply) => {
          const userInfo = await fetchOtherUserInfo(reply.authorID);
          let replyToUsername: string | undefined;
          if (reply.replyToUserID ) replyToUsername = (await fetchOtherUserInfo(reply.replyToUserID)).username;
          return { 
            ...reply, 
            userInfo, 
            replyToUsername,
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
      const limit=Math.min(10,comment.replyCount-(comment.replies?.length||0));
      if(limit<=0)return;

      //fetch server replies
      const serverReplies = comment.replies.filter(reply => !reply.isLocal);
      if (serverReplies.length === 0) return;
      const lastReply = serverReplies[serverReplies.length - 1];

      const newReplies = await new Promise<Comment[]>((resolve, reject) => {
        new QueryVideoCommentsMessage(
          parseInt(video_id || "0"),
          new Date(lastReply.timestamp).getTime(),
          lastReply.commentID,
          comment.commentID,
          limit
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
      console.log("获取到的回复",newReplies);
      if (newReplies.length === 0) {
        setComments(prev => prev.map(c => {
          if (c.commentID === comment.commentID) {
            return Object.assign(Object.create(Object.getPrototypeOf(c)), {
              ...c,
              hasMoreReplies: false,   
            });
          }
          return c;
        }));
        return;
      }


      const repliesWithUserInfo = await Promise.all(
        newReplies.map(async (reply) => {
          const userInfo = await fetchOtherUserInfo(reply.authorID);
          let replyToUsername: string | undefined;
          if (reply.replyToUserID ) replyToUsername = (await fetchOtherUserInfo(reply.replyToUserID)).username;
          return { 
            ...reply, 
            userInfo, 
            replyToUsername,
            isLocal:false,
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
          replyCount:0,
          isLocal:true,
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
  const handlePostReply = async (newComment:Comment) => {
    console.log("用户回复了新评论",newComment)
    const newReply:CommentWithUserInfo=newComment;
    newReply.userInfo=userInfo;
    newReply.isLiked=false;
    newReply.replyToUsername=replyingTo.username;
    newReply.isLocal=true;
    setComments((comments) => {
      return comments.map((comment) => {
        if (comment.commentID === replyingTo.id) {
            return {
              ...comment,
              replies: [newReply, ...(comment.replies || [])], // 新回复置顶
              replyCount: comment.replyCount + 1, // 总数+1
            };
          }
        if (comment.replies?.some((reply) => reply.commentID === replyingTo.id)) {
          const updatedReplies = [...comment.replies];
          const targetIndex = updatedReplies.findIndex(
            (reply) => reply.commentID === replyingTo.id
          );
          if (targetIndex !== -1) {
            updatedReplies.splice(targetIndex + 1, 0, newReply); // 在目标位置后插入
          }
          return Object.assign(Object.create(Object.getPrototypeOf(comment)), {
            ...comment,
            replies: updatedReplies,
            replyCount: comment.replyCount + 1,
          });
        }
        return comment;
      });
    });
  };

  const navigateToUser = (userID: number) => {
    navigate(`/home/${userID}`);
  };

  const followUp = (upID: number) => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }
    if(followisprocessing)return;
    setFollowisprocessing(true);
    upstat.followerCount = isFollowing ? upstat.followerCount - 1 : upstat.followerCount + 1;
    console.log("当前关注状态",isFollowing);
    new ChangeFollowStatusMessage(userToken,videoinfo.uploaderID, !isFollowing).send(
      () => {
        console.log("视频关注状态更新成功");
        setIsFollowing(!isFollowing);
        setFollowisprocessing(false);
      },
      (error: string) => {
        console.error("用户关注状态更新失败:", error);
      }
    );
  };

  const likeVideo = () => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }
    if(likeisprocessing) return; // 防止重复点击
    setLikeisprocessing(true);
    videoinfo.likes = isLiked ? videoinfo.likes - 1 : videoinfo.likes + 1;
    new ChangeLikeMessage(userToken, Number(video_id), !isLiked).send(
      () => {
        console.log("视频点赞状态更新成功");
        setIsLiked(!isLiked);
        setLikeisprocessing(false);
      },
      (error: string) => {
        console.error("视频点赞状态更新失败:", error);
      }
    );
    
    
  };

  const favoriteVideo = () => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }
    if(favoriteisprocessing) return; // 防止重复点击
    setFavoriteisprocessing(true);
    videoinfo.favorites = isFavorited ? videoinfo.favorites - 1 : videoinfo.favorites + 1;
    new ChangeFavoriteMessage(userToken, Number(video_id), !isFavorited).send(
      () => {
        console.log("视频收藏状态更新成功");
        setIsFavorited(!isFavorited);
        setFavoriteisprocessing(false);
      },
      (error: string) => {
        console.error("视频收藏状态更新失败:", error);
      }
    );
    
  };

  if (videoinfoisloading) {
    return // 骨架屏或加载动画
  }
  return (
    <div className="video-video-page">
      <Header />

      <div className="video-video-page-container">
        {/* Main content area */}
        <div className="video-video-main-content">
          {/* Video player section */}
          <div className="video-video-player-section">
            <h1 className="video-video-title">{videoinfo.title}</h1>

            <div className="video-video-meta">
              <span>播放: {videoinfo.views}</span>
              {/* <span>弹幕: {videoData.danmaku}</span> */}
              <span>投稿时间: {formatTime(videoinfo.uploadTime,false)}</span>
            </div>

            <div className="video-video-player-container">
              {/* 替换原有的 MinioVideoPlayer */}
              <HlsVideoPlayerWrapper videoID={Number(video_id)} />
            </div>

            <div className="video-video-actions">
              <button
                className={`video-videopage-action-btn ${isLiked ? 'liked' : ''}`}
                onClick={() => likeVideo()}
              >
                 {isLiked ? '点赞' : '点赞'}&nbsp;{videoinfo.likes}
              </button>
              <button
                className={`video-videopage-action-btn ${isFavorited ? 'favorited' : ''}`}
                onClick={() => favoriteVideo()}
              >
                 {isFavorited ? '收藏' : '收藏'}&nbsp;{videoinfo.favorites}
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
              <h3>评论 </h3>
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
                        {(comment.authorID === userInfo?.userID||userInfo?.userID === videoinfo?.uploaderID) && (
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
                                      {reply.replyToUserID && (
                                        <>
                                          回复&nbsp;
                                          <span 
                                            className="video-reply-highlight"
                                            onClick={() => navigateToUser(reply.replyToUserID)}
                                          >
                                            @{reply.replyToUsername}：
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
                                      {reply.replyToUserID && (
                                        <>
                                          回复&nbsp;
                                          <span 
                                            className="video-reply-highlight"
                                            onClick={() => navigateToUser(reply.replyToUserID)}
                                          >
                                            @{reply.replyToUsername}：
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
            {/* 头像和名字/签名在同一行 */}
            <div className="video-up-top-row">
              <div className="video-up-avatar">
                <img 
                src={uploaderInfo.avatarPath} 
                alt="UP主头像" 
                onClick={() => navigateToUser(uploaderInfo.userID)}
                />
              </div>
              <div className="video-up-details">
                <div className="video-up-name">
                  <span
                  onClick={()=>navigateToUser(uploaderInfo.userID)}
                  >
                    {uploaderInfo.username}
                  </span>
                </div>
                <div className="video-up-description" title={uploaderInfo.bio}>
                  {uploaderInfo.bio.length > 17
                    ? `${uploaderInfo.bio.substring(0, 17)}...`
                    : uploaderInfo.bio}
                </div>
              </div>
            </div>
            {/* 关注按钮单独一行 */}
            {((!userToken)||(videoinfo.uploaderID!==userInfo?.userID ))&&(
              <button
              className={`video-follow-btn ${isFollowing ? 'following' : ''}`}
              onClick={() => followUp(uploaderInfo.userID)}
            >
              {isFollowing ? '已关注' : '关注'}&nbsp;{upstat.followerCount}
            </button>
            )}
            
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
            onSuccess={(newComment:Comment) => {handlePostReply(newComment).then(()=>{setCommentInput(""),setShowReplyModal(false), setReplyingTo(null)})}}
        />
      )}
    </div>
  );
};

export default VideoPage;