import React, { useState, useEffect } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { QueryReplyNoticesMessage } from 'Plugins/MessageService/APIs/QueryReplyNoticesMessage';
import { ReplyNotice } from 'Plugins/MessageService/Objects/ReplyNotice';
import { useUserInfo } from 'Globals/GlobalStore';
import { fetchOtherUserInfo } from 'Globals/UserService';
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { formatTime } from 'Components/GetTime';
import ReplyModal from 'Components/ReplyModal/ReplyModal';
import "./MessagePage.css";
import "./ReplyTab.css";

const ReplyTab: React.FC = () => {
  interface ReplyWithUserInfo {
    replyNotice: ReplyNotice;
    userInfo: UserInfo;
  }

  const [replies, setReplies] = useState<ReplyWithUserInfo[]>([]);
  const [replyingComment, setReplyingComment] = useState<ReplyNotice | null>(null);
  const userToken = useUserToken();

  useEffect(() => {
    if (userToken) fetchRepliesWithUserInfo();
  }, [userToken]);

  const fetchRepliesWithUserInfo = async () => {
    try {
      const notices = await fetchReplies();
      const repliesWithUserInfo = await Promise.all(
        notices.map(async (reply) => {
          const userInfo = await fetchOtherUserInfo(reply.senderID);
          return { replyNotice: reply, userInfo };
        })
      );
      setReplies(repliesWithUserInfo);
    } catch (error) {
      console.error('加载失败:', error);
    }
  };

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

  const handleOriginalClick = (videoUrl: number) => {

  };

  return (
    <div className="system-container">
      <div className="system-header">
        <h3>回复我的</h3>
      </div>

      <div className="reply-list">
        {replies.map(reply => (
          <div key={reply.replyNotice.noticeID} className="reply-item">
            <div className="reply-user-section">
              <div className="user-avatar">
                <img
                  src={reply.userInfo.avatarPath || '/default-avatar.png'}
                  alt={reply.userInfo.username}
                />
              </div>
            </div>

            <div className="reply-content-section">
              <div className="reply-title">
                <span className="reply-username">{reply.userInfo.username}</span> 回复了我的评论
                {reply.replyNotice.originalContent && (
                  <span
                    className="reply-original-text"
                    onClick={() => handleOriginalClick(reply.replyNotice.videoID)}
                    title={reply.replyNotice.originalContent}
                  >
                    {reply.replyNotice.originalContent}
                  </span>
                )}
              </div>
              <div className="reply-text">
                {reply.replyNotice.content}
              </div>

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
            fetchRepliesWithUserInfo();
          }}
        />
      )}
    </div>
  );
};

export default ReplyTab;