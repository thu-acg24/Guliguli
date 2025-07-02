import React, { useState, useEffect } from 'react';
import { useUserToken } from '../../Globals/GlobalStore';
import { materialAlertError } from '../../Plugins/CommonUtils/Gadgets/AlertGadget';

const ReplyTab: React.FC = () => {
  const [replies, setReplies] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const userToken = useUserToken();

  useEffect(() => {
    if (userToken) fetchReplies();
  }, [userToken]);

  const fetchReplies = async () => {
    setLoading(true);
    try {
      setReplies([
        { 
          id: '1', 
          user: { name: '用户A' }, 
          content: '这个视频很棒！', 
          time: '2小时前', 
          liked: false,
          likes: 5,
          originalComment: '关于React的教程非常实用...'
        },
        { 
          id: '2', 
          user: { name: '用户B' }, 
          content: '学到了很多新知识', 
          time: '昨天', 
          liked: true,
          likes: 12,
          originalComment: '前端开发的最佳实践...'
        }
      ]);
    } catch (error) {
      materialAlertError('加载失败', error.message);
    } finally {
      setLoading(false);
    }
  };

  const toggleLike = (replyId: string) => {
    setReplies(replies.map(reply => 
      reply.id === replyId ? { 
        ...reply, 
        liked: !reply.liked,
        likes: reply.liked ? reply.likes - 1 : reply.likes + 1
      } : reply
    ));
  };

  return (
    <div className="reply-container">
      <div className="reply-header">
        <h3>回复我的</h3>
      </div>
      
      <div className="reply-list">
        {replies.map(reply => (
          <div key={reply.id} className="reply-item">
            <div className="reply-user">
              <div className="user-avatar">
                <img src={`data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWReLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI3IiByPSI0Ii8+PC9zdmc+`} alt="头像" />
              </div>
              <div className="user-name">{reply.user.name}</div>
            </div>
            
            <div className="reply-content">
              <div className="reply-text">{reply.content}</div>
              
              <div className="reply-meta">
                <span className="reply-time">{reply.time}</span>
                
                <div className="action-buttons">
                  <button 
                    className={`like-btn ${reply.liked ? 'liked' : ''}`}
                    onClick={() => toggleLike(reply.id)}
                  >
                    {reply.liked ? '已赞' : '点赞'} ({reply.likes})
                  </button>
                  
                  <button 
                    className="reply-btn"
                    onClick={() => alert('回复功能')}
                  >
                    回复
                  </button>
                </div>
              </div>
            </div>
            
            <div className="original-comment">
              {reply.originalComment}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ReplyTab;