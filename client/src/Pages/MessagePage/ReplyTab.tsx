import React, { useState, useEffect } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { QueryReplyNoticesMessage } from 'Plugins/MessageService/APIs/QueryReplyNoticesMessage';
import { ReplyNotice } from 'Plugins/MessageService/Objects/ReplyNotice';
import { useUserInfo } from 'Hooks/useUseInfo';
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { formatTime } from 'Components/GetTime';
import ReplyModal from 'Components/ReplyModal/ReplyModal';
import "./MessagePage.css"; 
import './ReplyTab.module.css'

const ReplyTab: React.FC = () => {
  interface ReplyWithUserInfo{
    replyNotice: ReplyNotice;
    userInfo: UserInfo; 
  }
  const [replies, setReplies] = useState<ReplyWithUserInfo[]>([]);
  const [replyingComment, setReplyingComment] = useState<ReplyNotice | null>(null);
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
      
      <div className="reply-list-container">
        {replies.map(reply => (
          <div key={reply.replyNotice.noticeID} className="reply-item">
            <div className="reply-user-section">
              <div className="reply-avatar">
                <img 
                  src={reply.userInfo.avatarPath || '/default-avatar.png'} 
                  alt={reply.userInfo.username}
                />
              </div>
              <div className="reply-username">
                {reply.userInfo.username}
              </div>
            </div>
            
            <div className="reply-content-section">
              <div className="reply-main-text">
                {reply.replyNotice.content}
              </div>
              
              {reply.replyNotice.originalContent && (
                <div className="reply-original-wrapper">
                  <div className="reply-original-text">
                    {reply.replyNotice.originalContent}
                  </div>
                </div>
              )}
              
              <div className="reply-footer">
                <span className="reply-time">
                  {formatTime(reply.replyNotice.timestamp)}
                </span>
                <button 
                  className="reply-action-btn"
                  onClick={() => setReplyingComment(reply.replyNotice)}
                >
                  回复
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>


      {replyingComment && (
        <ReplyModal
          replyingComment={replyingComment}
          onClose={() => setReplyingComment(null)}
          onSuccess={() => {
            // 可以添加刷新列表的逻辑
          }}
        />
      )}
    </div>
  );
};

export default ReplyTab;