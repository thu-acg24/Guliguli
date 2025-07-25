import React, { useState } from "react";  // 确保包含 useState
import ReplyItem from "./ReplyItem";
import "./VideoPage.css";
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { CommentWithUserInfo } from './VideoPage'
import { CommentReportModal } from "./ReportComponents";
import { Video } from 'Plugins/VideoService/Objects/Video';
import { formatTime } from 'Components/Formatter';
import { ThreeDotsIcon, LikeIcon } from 'Images/Icons';

const CommentItem: React.FC<{
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
}> = ({
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
              {!comment.showAllReplies && comment.replies && comment.replies.length > 0 && (

                <div className="video-replies-list">

                  {comment.replies.slice(0, 2).map(reply => (
                    <ReplyItem
                      key={reply.commentID}
                      reply={reply}
                      isLoggedIn={isLoggedIn}
                      userInfo={userInfo}
                      videoInfo={videoInfo}
                      handleLikeComment={handleLikeComment}
                      handleDeleteComment={handleDeleteComment}
                      navigateToUser={navigateToUser}
                      setReplyingTo={setReplyingTo}
                      setShowReplyModal={setShowReplyModal}
                      setShowLoginModal={setShowLoginModal}
                    />
                  ))}
                </div>
              )}
              {comment.showAllReplies && comment.replies && comment.replies.length > 0 && (
                <div className="video-replies-list">
                  {comment.replies.map(reply => (
                    <ReplyItem
                      key={reply.commentID}
                      reply={reply}
                      isLoggedIn={isLoggedIn}
                      userInfo={userInfo}
                      videoInfo={videoInfo}
                      handleLikeComment={handleLikeComment}
                      handleDeleteComment={handleDeleteComment}
                      navigateToUser={navigateToUser}
                      setReplyingTo={setReplyingTo}
                      setShowReplyModal={setShowReplyModal}
                      setShowLoginModal={setShowLoginModal}
                    />
                  ))}
                </div>
              )}
              <div className="video-reply-actions">
                {comment.replyCount > 0 && (
                  <span
                    className="video-view-replies"
                    onClick={() => handleToggleReplies(comment)}
                  >
                    {comment.showAllReplies ? '收起' : ((comment.replies?.length | 0) > 2 ? `查看剩余${comment.replyCount - 2}条回复` : ``)}
                  </span>
                )}
                {comment.showAllReplies && comment.hasMoreReplies && (//展开了后
                  <span
                    className="video-load-more-replies"
                    onClick={() => handleLoadMoreReplies(comment)}
                  >
                    查看剩余{comment.replyCount - comment.replies.length}条回复
                  </span>
                )}
              </div>
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