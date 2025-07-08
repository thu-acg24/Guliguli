import React, { useRef } from "react";
import CommentItem from "./CommentItem";
import {CommentWithUserInfo} from './VideoPage'
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import {Video} from 'Plugins/VideoService/Objects/Video'
import defaultavatar from "Images/DefaultAvatar.jpg"
import "./VideoPage.css";

interface CommentSectionProps {
  comments: CommentWithUserInfo[];
  loadingComments: boolean;
  noMoreComments: boolean;
  isLoggedIn: boolean;
  userInfo: UserInfo;
  commentInput: string;
  videoInfo:Video;
  setCommentInput: (value: string) => void;
  handlePostComment: () => void;
  handleLoadMore: () => void;
  handleLikeComment: (id: number) => void;
  handleDeleteComment: (id: number) => void;
  navigateToUser: (id: number) => void;
  setReplyingTo: (value: { id: number, username: string, content: string } | null) => void;
  setShowReplyModal: (value: boolean) => void;
  setShowLoginModal: (value: boolean) => void;
  handleToggleReplies: (comment: CommentWithUserInfo) => void;
  handleLoadMoreReplies: (comment: CommentWithUserInfo) => void;
}

const CommentSection: React.FC<CommentSectionProps> = ({
  comments,
  loadingComments,
  noMoreComments,
  isLoggedIn,
  userInfo,
  commentInput,
  videoInfo,
  setCommentInput,
  handlePostComment,
  handleLoadMore,
  handleLikeComment,
  handleDeleteComment,
  navigateToUser,
  setReplyingTo,
  setShowReplyModal,
  setShowLoginModal,
  handleToggleReplies,
  handleLoadMoreReplies
}) => {
  const commentsSectionRef = useRef<HTMLDivElement>(null);
  const commentInputRef = useRef<HTMLDivElement>(null);

  return (
    <div className="video-comments-container" ref={commentsSectionRef}>
      <div className="video-comments-header">
        <h3>评论</h3>
      </div>

      <div className="video-comment-input-area" ref={commentInputRef}>
        <img
          src={isLoggedIn ? (userInfo?.avatarPath || defaultavatar) : defaultavatar}
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

      <div className="video-comments-list">
        {comments.map(comment => (
          <CommentItem
            key={comment.commentID}
            comment={comment}
            isLoggedIn={isLoggedIn}
            userInfo={userInfo}
            videoInfo={videoInfo}
            handleLikeComment={handleLikeComment}
            handleDeleteComment={handleDeleteComment}
            navigateToUser={navigateToUser}
            setReplyingTo={setReplyingTo}
            setShowReplyModal={setShowReplyModal}
            setShowLoginModal={setShowLoginModal}
            handleToggleReplies={handleToggleReplies}
            handleLoadMoreReplies={handleLoadMoreReplies}
            
          />
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
  );
};

export default CommentSection;