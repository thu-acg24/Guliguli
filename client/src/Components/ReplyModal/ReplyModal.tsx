import React, { useState } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { Comment } from 'Plugins/CommentService/Objects/Comment';
import { PublishCommentMessage } from 'Plugins/CommentService/APIs/PublishCommentMessage';
import './ReplyModal.css';

interface ReplyModalProps {
  commentID: number;
  videoID: number;
  replyingToContent:string|null;
  content: string;
  onClose: () => void;
  onSuccess?: (newComment: Comment) => void;
}
//只需要commentID, videoID, content
const ReplyModal: React.FC<ReplyModalProps> = ({ commentID, videoID, replyingToContent,content, onClose, onSuccess }) => {
  const [replyContent, setReplyContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const userToken = useUserToken();

  const handleSubmit = async () => {
    if (!replyContent.trim()) return;
    setIsSubmitting(true);
    try {
      new Promise<Comment>((resolve, reject) => {
        new PublishCommentMessage(
          userToken, 
          videoID, 
          replyContent, 
          commentID
        ).send(
          (info:string) => {
            try {
              const data: Comment = JSON.parse(info);
              resolve(data);
              onSuccess(data)
            } catch (e) {
              reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
            }
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

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit();
    }
  };

  return (
    <div className="replymodal-overlay">
      <div className="replymodal-main">
        <div className="replymodal-header">
          <h3 className="replymodal-title">回复评论</h3>
          <button 
            className="replymodal-close"
            onClick={onClose}
            aria-label="关闭弹窗"
          >
            ×
          </button>
        </div>
        
        <div className="replymodal-body">
        
          <div className="replymodal-original-comment">
            <span className="replymodal-original-label">评论：{replyingToContent}</span>
            {content}
          </div>
          <textarea
            className="replymodal-textarea"
            value={replyContent}
            onChange={(e) => setReplyContent(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="请输入回复内容..."
            autoFocus
          />
        </div>
        
        <div className="replymodal-footer">
          <button 
            className="replymodal-cancel"
            onClick={onClose}
            disabled={isSubmitting}
          >
            取消
          </button>
          <button
            className="replymodal-submit"
            onClick={handleSubmit}
            disabled={isSubmitting || !replyContent.trim()}
          >
            {isSubmitting ? (
              <>
                <span className="replymodal-spinner" />
                发送中...
              </>
            ) : '发送回复'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ReplyModal;