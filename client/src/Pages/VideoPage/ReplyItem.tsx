import React, { useState } from "react";  // 确保包含 useState
import "./VideoPage.css";
import { CommentWithUserInfo } from './VideoPage'
import { CommentReportModal } from "./ReportComponents";
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { Video } from "Plugins/VideoService/Objects/Video";
import { formatTime } from 'Components/Formatter';
import { ThreeDotsIcon, LikeIcon } from 'Images/Icons';

interface ReplyItemProps {
  reply: CommentWithUserInfo;
  isLoggedIn: boolean;
  userInfo: UserInfo;
  videoInfo: Video;
  handleLikeComment: (id: number) => void;
  handleDeleteComment: (id: number) => void;
  navigateToUser: (id: number) => void;
  setReplyingTo: (value: { id: number, username: string, content: string } | null) => void;
  setShowReplyModal: (value: boolean) => void;
  setShowLoginModal: (value: boolean) => void;
}

const ReplyItem: React.FC<ReplyItemProps> = ({
  reply,
  isLoggedIn,
  userInfo,
  videoInfo,
  handleLikeComment,
  handleDeleteComment,
  navigateToUser,
  setReplyingTo,
  setShowReplyModal,
  setShowLoginModal,
}) => {
  const [showOptions, setShowOptions] = React.useState(false);
  const [showCommentReport, setShowCommentReport] = useState(false);

  return (
    <div className="video-reply-item">
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
            <LikeIcon className="video-like-icon" />
            <span>{reply.likes}</span>
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
          
          <div 
            className="video-reply-options"
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
                {(reply.authorID === userInfo?.userID || userInfo?.userID === videoInfo?.uploaderID) && (
                  <button
                    className="video-delete-btn"
                    onClick={() => handleDeleteComment(reply.commentID)}
                  >
                    删除
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
      {showCommentReport && (
        <CommentReportModal
          visible={showCommentReport}
          onCancel={() => setShowCommentReport(false)}
          commentID={reply.commentID}
        />
      )}
    </div>
  );
};

export default ReplyItem;