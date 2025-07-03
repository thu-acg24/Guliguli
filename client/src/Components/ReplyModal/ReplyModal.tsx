import React, { useState } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { PublishCommentMessage } from 'Plugins/CommentService/APIs/PublishCommentMessage';
import { ReplyNotice } from 'Plugins/MessageService/Objects/ReplyNotice';
import './ReplyModal.module.css'; // 确保有对应的CSS样式文件
interface ReplyModalProps {
  replyingComment: ReplyNotice;
  onClose: () => void;
  onSuccess?: () => void;
}

const ReplyModal: React.FC<ReplyModalProps> = ({ replyingComment, onClose, onSuccess }) => {
  const [replyContent, setReplyContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const userToken = useUserToken();

  const handleSubmit = async () => {
    if (!replyContent.trim()) return;
    
    setIsSubmitting(true);
    try {
      await new Promise((resolve, reject) => {
        new PublishCommentMessage(userToken, replyingComment.videoID, replyContent, replyingComment.commentID).send(
          () => {
            resolve(true);
            onSuccess?.();
            onClose();
          },
          (error) => {
            reject(error);
            alert(`回复失败: ${error}`);
          }
        );
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="reply-modal-overlay">
      <div className="reply-modal-main">
        <div className="reply-modal-header">
          <h3 className="reply-modal-title">回复评论</h3>
          <button className="reply-modal-close" onClick={onClose}>×</button>
        </div>
        
        <div className="reply-modal-body">
          <textarea
            className="reply-modal-textarea"
            value={replyContent}
            onChange={(e) => setReplyContent(e.target.value)}
            placeholder="请输入回复内容..."
          />
        </div>
        
        <div className="reply-modal-footer">
          <button 
            className="reply-modal-cancel"
            onClick={onClose}
            disabled={isSubmitting}
          >
            取消
          </button>
          <button
            className="reply-modal-submit"
            onClick={handleSubmit}
            disabled={isSubmitting || !replyContent.trim()}
          >
            {isSubmitting ? '发送中...' : '发送回复'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ReplyModal;