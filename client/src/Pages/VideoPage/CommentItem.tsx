import React, { useState } from "react";  // 确保包含 useState
import ReplyItem from "./ReplyItem";
import "./VideoPage.css";
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { CommentWithUserInfo } from './VideoPage'
import { CommentReportModal } from "./ReportComponents";
import { Video } from 'Plugins/VideoService/Objects/Video';
import { formatTime } from 'Components/Formatter';
import { ThreeDotsIcon, LikeIcon } from 'Images/Icons';

interface CommentItemProps {
  comment: CommentWithUserInfo;
  isLoggedIn: boolean;
  userInfo: UserInfo;
  videoInfo: Video;
  handleLikeComment: (id: number) => void;
  handleDeleteComment: (id: number) => void;
  navigateToUser: (id: number) => void;
  setReplyingTo: (value: { id: number, username: string, content: string } | null) => void;
  setShowReplyModal: (value: boolean) => void;
  setShowLoginModal: (value: boolean) => void;
  handleToggleReplies: (comment: CommentWithUserInfo) => void;
  handleLoadMoreReplies: (comment: CommentWithUserInfo) => void;
}

const CommentItem: React.FC<CommentItemProps> = ({
  comment,
  isLoggedIn,
  userInfo,
  videoInfo,
  handleLikeComment,
  handleDeleteComment,
  navigateToUser,
  setReplyingTo,
  setShowReplyModal,
  setShowLoginModal,
  handleToggleReplies,
  handleLoadMoreReplies
}) => {
  const [showOptions, setShowOptions] = React.useState(false);
  const [showCommentReport, setShowCommentReport] = useState(false);
  return (
    <div className="video-comment-item">
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
              <LikeIcon className="video-like-icon" />
              <span>{comment.likes}</span>
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
            
            <div 
              className="video-comment-options"
              onMouseEnter={() => setShowOptions(true)}
              onMouseLeave={() => setShowOptions(false)}
            >
              <ThreeDotsIcon className="video-options-icon" />
              {showOptions && (
                <div className="video-options-menu">
                  <button 
                    className="video-report-btn"
                    onClick={() => isLoggedIn ? setShowCommentReport(true) : setShowLoginModal(true)}
                  >
                    举报
                  </button>
                  {(comment.authorID === userInfo?.userID || userInfo?.userID === videoInfo?.uploaderID) && (
                    <button
                      className="video-delete-btn"
                      onClick={() => handleDeleteComment(comment.commentID)}
                    >
                      删除
                    </button>
                  )}
                </div>
              )}
            </div>
          </div>

          {comment.replyCount > 0 && (
            <div className="video-replies-section">
              {/* ... rest of the reply section remains the same ... */}
            </div>
          )}
        </div>
      </div>
      {showCommentReport && (
        <CommentReportModal
          visible={showCommentReport}
          onCancel={() => setShowCommentReport(false)}
          commentID={comment.commentID}
        />
      )}
    </div>
  );
};

export default CommentItem;