import React, { useState, useEffect, useLayoutEffect, useRef, useCallback } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useNavigateHome } from "Globals/Navigate";
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
import { formatTime } from 'Components/Formatter';
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { Video } from 'Plugins/VideoService/Objects/Video';
import { QueryVideoInfoMessage } from 'Plugins/VideoService/APIs/QueryVideoInfoMessage';
import { QueryFavoriteMessage } from 'Plugins/VideoService/APIs/QueryFavoriteMessage';
import { QueryLikeMessage } from 'Plugins/VideoService/APIs/QueryLikeMessage';
import { ChangeLikeMessage } from 'Plugins/VideoService/APIs/ChangeLikeMessage';
import { ChangeFavoriteMessage } from 'Plugins/VideoService/APIs/ChangeFavoriteMessage';
import { UserStat } from 'Plugins/UserService/Objects/UserStat';
import { QueryUserStatMessage } from 'Plugins/UserService/APIs/QueryUserStatMessage';
import { QueryFollowMessage } from 'Plugins/UserService/APIs/QueryFollowMessage';
import { ChangeFollowStatusMessage } from 'Plugins/UserService/APIs/ChangeFollowStatusMessage';
import VideoPlayerSection from "./VideoPlayerSection";
import CommentSection from "./CommentSection";
import SidebarSection from "./SidebarSection";
import { getRecommendedVideos, SimpleVideo } from "Components/RecommendVideoService";
import { VideoStatus } from "Plugins/VideoService/Objects/VideoStatus"
import "./VideoPage.css";

export const videoPagePath = "/video/:video_id";


export function useNavigateVideo() {
  const navigate = useNavigate();

  const navigateVideo = useCallback((video_id: string | number) => {
    navigate(videoPagePath.replace(":video_id", String(video_id)));
  }, [navigate]);

  return { navigateVideo };
}

export interface CommentWithUserInfo extends Comment {
  isLocal?: boolean;
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
  const { navigateHome } = useNavigateHome();
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
  const commentInputRef = useRef<HTMLDivElement>(null);
  const [isFollowing, setIsFollowing] = useState(false);
  const [isLiked, setIsLiked] = useState(false);
  const [isFavorited, setIsFavorited] = useState(false);
  const [likeisprocessing, setLikeisprocessing] = useState(false);
  const [followisprocessing, setFollowisprocessing] = useState(false);
  const [favoriteisprocessing, setFavoriteisprocessing] = useState(false);
  const [videoInfo, setVideoinfo] = useState<Video>(null);
  const [recommendvideosInfo, setRecommendvideosInfo] = useState<SimpleVideo[]>([]);
  const [videosisloading, setVideosisloading] = useState(true);
  const [upstat, setUpstat] = useState<UserStat>();


  useEffect(() => {
    setVideosisloading(true);
    getRecommendedVideos(userToken ? userToken : null, (videoInfo?.status === VideoStatus.approved ? Number(video_id) : null), 4).then(setRecommendvideosInfo).then(() => { setVideosisloading(false) });
  }, [userToken]);
  useLayoutEffect(() => {
    console.log("现在正在看的是", video_id);
    setVideoinfoIsLoading(true);
    window.scrollTo(0, 0);
    fetchVideoInfo();
  }, [video_id]);
  useEffect(() => {
    setIsLoggedIn(!!userToken);
    setComments([]);
    setNoMoreComments(false);
    setCommentInput("");
    setReplyingTo(null);
    fetchComments();
  }, [userToken, video_id]);

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

    if (!userToken || !videoInfo || videoInfo?.status !== VideoStatus.approved) return;
    console.log("开始获取用户点赞收藏关注");
    console.log("当前视频信息", videoInfo);
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
    if (userInfo.userID === videoInfo.uploaderID) return;
    new QueryFollowMessage(userInfo.userID, videoInfo.uploaderID).send(
      (info: string) => {
        const isFollowing = JSON.parse(info);
        setIsFollowing(isFollowing);
      },
      (error: string) => {
        console.error("获取用户关注状态失败:", error);
      }
    );
  }, [userInfo, videoInfo]);
  const fetchVideoInfo = async () => {
    if (!video_id) return;
    try {
      const videoInfo = await new Promise<Video>((resolve, reject) => {
        new QueryVideoInfoMessage(userToken ? userToken : null, Number(video_id)).send(
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

      const upStat = await new Promise<UserStat>((resolve, reject) => {
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
    } finally {
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
      await Promise.all(//先一步获取适当评论
        commentsWithUserInfo.map(async (comment) => {
          console.log("即将要进去获取的回复", comment);
          await handleLoadMoreReplies(comment);
          return;
        })
      );
    } catch (error) {
      console.error('加载评论失败:', error);
    } finally {
      setLoadingComments(false);
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

  const handleToggleReplies = async (comment: CommentWithUserInfo) => { //切换收起和展开的状态

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
    try {
      const limit = Math.min(10, comment.replyCount - (comment.replies?.length || 0));
      if (limit <= 0) return;
      //fetch server replies
      const serverReplies = comment.replies.filter(reply => !reply.isLocal);
      const lastReplytimestamp = (serverReplies.length === 0 ? "1900-12-31 23:59:59" : serverReplies[serverReplies.length - 1].timestamp);
      const lastReplycommentID = (serverReplies.length === 0 ? 0 : serverReplies[serverReplies.length - 1].commentID);

      const newReplies = await new Promise<Comment[]>((resolve, reject) => {
        new QueryVideoCommentsMessage(
          parseInt(video_id || "0"),
          new Date(lastReplytimestamp).getTime(),
          lastReplycommentID,
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
      console.log("获取到的回复", newReplies);
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
          if (reply.replyToUserID) replyToUsername = (await fetchOtherUserInfo(reply.replyToUserID)).username;
          return {
            ...reply,
            userInfo,
            replyToUsername,
            isLocal: false,
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
      console.log("当前评论", comment);
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
          replyCount: 0,
          isLocal: true,
          showAllReplies: true,
          hasMoreReplies: false
        }),
        ...prev
      ]);

      setCommentInput("");
    } catch (error) {
      console.error('发布评论失败:', error);
    }
  };
  const handlePostReply = async (newComment: Comment) => {
    console.log("用户回复了新评论", newComment)
    const newReply: CommentWithUserInfo = newComment;
    newReply.userInfo = userInfo;
    newReply.isLiked = false;
    newReply.replyToUsername = replyingTo.username;
    newReply.isLocal = true;
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
    navigateHome(userID);
  };

  const followUp = (upID: number) => {
    if (!isLoggedIn) {
      setShowLoginModal(true);
      return;
    }
    if (followisprocessing) return;
    setFollowisprocessing(true);
    upstat.followerCount = isFollowing ? upstat.followerCount - 1 : upstat.followerCount + 1;
    console.log("当前关注状态", isFollowing);
    new ChangeFollowStatusMessage(userToken, videoInfo.uploaderID, !isFollowing).send(
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
    if (likeisprocessing) return; // 防止重复点击
    if (videoInfo.status !== VideoStatus.approved) return;
    setLikeisprocessing(true);
    videoInfo.likes = isLiked ? videoInfo.likes - 1 : videoInfo.likes + 1;
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
    if (favoriteisprocessing) return; // 防止重复点击
    if (videoInfo.status !== VideoStatus.approved) return;
    setFavoriteisprocessing(true);
    videoInfo.favorites = isFavorited ? videoInfo.favorites - 1 : videoInfo.favorites + 1;
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

  if (videoinfoisloading || videosisloading) {
    return // 骨架屏或加载动画
  }

  return (
    <div className="video-video-page">
      <Header />

      <div className="video-video-page-container">
        <div className="video-video-main-content">
          <VideoPlayerSection
            video_id={video_id}
            videoInfo={videoInfo}
            isLiked={isLiked}
            isFavorited={isFavorited}
            likeVideo={likeVideo}
            favoriteVideo={favoriteVideo}
            isLoggedIn={isLoggedIn}
            setShowLoginModal={setShowLoginModal}
          />
          {videoInfo?.status === VideoStatus.approved &&
            <CommentSection
              comments={comments}
              loadingComments={loadingComments}
              noMoreComments={noMoreComments}
              isLoggedIn={isLoggedIn}
              userInfo={userInfo}
              commentInput={commentInput}
              videoInfo={videoInfo}
              setCommentInput={setCommentInput}
              handlePostComment={handlePostComment}
              handleLoadMore={handleLoadMore}
              handleLikeComment={handleLikeComment}
              handleDeleteComment={handleDeleteComment}
              navigateToUser={navigateToUser}
              setReplyingTo={setReplyingTo}
              setShowReplyModal={setShowReplyModal}
              setShowLoginModal={setShowLoginModal}
              handleToggleReplies={handleToggleReplies}
              handleLoadMoreReplies={handleLoadMoreReplies}
            />
          }
        </div>

        <SidebarSection
          uploaderInfo={uploaderInfo}
          videoInfo={videoInfo}
          userToken={userToken}
          userInfo={userInfo}
          isFollowing={isFollowing}
          upstat={upstat}
          followUp={followUp}
          navigateToUser={navigateToUser}
          recommendedVideos={recommendvideosInfo}
        />
      </div>

      {showBottomCommentBar && videoInfo.status === VideoStatus.approved && (
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

      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
      />

      {showReplyModal && replyingTo && (
        <ReplyModal
          videoID={parseInt(video_id || "0")}
          commentID={replyingTo.id}
          replyingToContent={replyingTo.content}
          content={commentInput}
          onClose={() => { setCommentInput(""), setShowReplyModal(false), setReplyingTo(null) }}
          onSuccess={(newComment: Comment) => { handlePostReply(newComment).then(() => { setCommentInput(""), setShowReplyModal(false), setReplyingTo(null) }) }}
        />
      )}
    </div>
  );
};

export default VideoPage;