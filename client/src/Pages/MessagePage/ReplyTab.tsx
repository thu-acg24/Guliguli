import React, { useState, useEffect } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { QueryReplyNoticesMessage } from 'Plugins/MessageService/APIs/QueryReplyNoticesMessage';
import { ReplyNotice } from 'Plugins/MessageService/Objects/ReplyNotice';
import { useUserInfo } from 'Hooks/useUseInfo';
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { formatTime } from 'Components/GetTime';
const ReplyTab: React.FC = () => {
  interface ReplyWithUserInfo{
    replyNotice: ReplyNotice;
    userInfo: UserInfo; 
  }
  const [replies, setReplies] = useState<ReplyWithUserInfo[]>([]);
  const userToken = useUserToken();
  const { fetchOtherUserInfo } = useUserInfo();
  useEffect(() => {
    if (userToken) fetchRepliesWithUserInfo();
  }, [userToken]);
  const fetchRepliesWithUserInfo = async () => {
    try {
      const notices = await fetchReplies();

      const repliesWithUserInfo = await Promise.all(
        notices.map(async (reply) => {
          const userInfo = await fetchOtherUserInfo(reply.senderID);
          return {replyNotice: reply, userInfo }; // 合并用户信息到回复对象
        })
      );
      setReplies(repliesWithUserInfo);
    } catch (error) {
      console.error('加载失败:', error);
    } finally {}
  };

  // 原始获取回复的函数（保持不变）
  const fetchReplies = async (): Promise<ReplyNotice[]> => {
    return new Promise((resolve, reject) => {
      new QueryReplyNoticesMessage(userToken).send(
        (info: string) => {
          try {
            const data: ReplyNotice[] = JSON.parse(info);
            resolve(data);
          } catch (e) {
            reject(new Error(`解析失败: ${e instanceof Error ? e.message : String(e)}`));
          }
        },
        (e: string) => reject(new Error(`请求失败: ${e}`))
      );
    });
  };

  return (
    <div className="reply-container">
      <div className="reply-header">
        <h3>回复我的</h3>
      </div>
      
      <div className="reply-list">
        {replies.map(reply => (
          <div key={reply.replyNotice.noticeID} className="reply-item">
            <div className="reply-user">
              <div className="user-avatar">
                <img src={reply.userInfo.avatarPath} alt="头像" />
              </div>
              <div className="user-name">{reply.userInfo.username}</div>
            </div>
            
            <div className="reply-content">
              <div className="reply-text">{reply.replyNotice.content}</div>
              
              <div className="reply-meta">
                <span className="reply-time">{
                                formatTime(reply.replyNotice.timestamp)}</span>

                <div className="action-buttons">
                  
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
              {reply.replyNotice.originalContent}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ReplyTab;