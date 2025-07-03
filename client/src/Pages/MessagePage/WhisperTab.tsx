import React, { useState, useEffect, useRef } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { materialAlertError } from 'Plugins/CommonUtils/Gadgets/AlertGadget';

import { QueryMessagesMessage } from 'Plugins/MessageService/APIs/QueryMessagesMessage';
import { SendMessageMessage } from 'Plugins/MessageService/APIs/SendMessageMessage';
import { QueryUserInContactMessage } from 'Plugins/MessageService/APIs/QueryUserInContactMessage';
import { Message } from 'Plugins/MessageService/Objects/Message';
import { UserInfoWithMessage } from 'Plugins/MessageService/Objects/UserInfoWithMessage';
import { useUserInfo } from 'Hooks/useUseInfo';
import { formatTime } from 'Components/GetTime';


const WhisperTab: React.FC = () => {
  const [conversations, setConversations] = useState<UserInfoWithMessage[]>([]);
  const [selectedUser, setSelectedUser] = useState<number | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageInput, setMessageInput] = useState('');
  const messageEndRef = useRef<HTMLDivElement>(null);
  const userToken = useUserToken();
  // 在组件顶部添加新状态保存刷新前选中的用户
  const [refreshFlag, setRefreshFlag] = useState(false);
  const { userInfo, fetchUserInfo, getUserIDByToken } = useUserInfo();
  // 添加useEffect处理刷新
  useEffect(() => {
    fetchConversations();
    if (selectedUser) fetchMessages(selectedUser);
  }, [refreshFlag]);

  useEffect(() => {
    if (userToken) {
      getUserIDByToken(userToken).then(userID => fetchUserInfo(userID)).then(() => {
        fetchConversations();
      });
    }
  }, [userToken]);

  useEffect(() => {
    if (selectedUser) {
      const loadData = async () => {
        if (selectedUser) {
          await fetchMessages(selectedUser); // 等待完成
          await fetchConversations();
        }
      };
      loadData();
    }
  }, [selectedUser]);

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const fetchConversations = async () => {
    try {
      await new Promise<void>((resolve, reject) => {
        new QueryUserInContactMessage(userToken).send(
          (info: string) => {
            try {
              const data = JSON.parse(info);
              setConversations(data);
              resolve(); 
            } catch (e) {
              reject(e);
            }
          },
          (e: string) => reject(new Error(e)) 
        );
      });
    } catch (error) {
      materialAlertError('加载对话列表失败', error.message);
    } finally {
    }
  };

  const fetchMessages = async (userID: number): Promise<void> => {
    try {
      // 将回调式API转换为Promise
      const messages = await new Promise<Message[]>((resolve, reject) => {
        new QueryMessagesMessage(userToken, userID).send(
          (info: string) => {
            try {
              const data: Message[] = JSON.parse(info);
              resolve(data); // 成功时返回解析的数据
            } catch (e) {
              reject(new Error("消息解析失败")); // JSON解析错误
            }
          },
          (e: string) => {
            reject(new Error(e)); // 网络或业务错误
          }
        );
      });
      setMessages(messages);

    } catch (error) {
      materialAlertError('加载消息失败', error.message);
    } finally {
    }
  };

  const handleSendMessage = async (): Promise<void> => {
    if (!messageInput.trim() || !selectedUser) return;
    try {
      sessionStorage.setItem('selectedUser', selectedUser.toString());
      await new Promise<void>((resolve, reject) => {
        new SendMessageMessage(userToken, selectedUser, messageInput).send(
          () => {
            resolve(); // 发送成功
          },
          (e: string) => {
            reject(new Error(e)); // 发送失败
          }
        );
      });
      setRefreshFlag(prev => !prev); // 使用函数式更新确保最新状态
      setMessageInput("");

    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : '发送消息失败';
      materialAlertError('发送消息失败', errorMessage);
    }
  };
  return (
    <div className="whisper-container">
      <div className="user-list">
        <div className="user-list-header">
          <h3>我的消息</h3>
          <button className="new-chat-btn">新建聊天</button>
        </div>

        {conversations.map(conversation => {
          const user = conversation.userInfo;
          return (
            <div
              key={user.userID}
              className={`user-item ${selectedUser === user.userID ? 'active' : ''}`}
              onClick={() => !user.isBanned && setSelectedUser(user.userID)}
            >
              <div className="user-avatar">
                <img src={user.avatarPath} alt="头像" />
              </div>
              {conversation.unreadCount > 0 && (
                <div
                  className="unread-count"
                  data-count={conversation.unreadCount > 99 ? "99+" : conversation.unreadCount}
                >
                  {conversation.unreadCount <= 99 ? conversation.unreadCount : null}
                </div>
              )}
              <div className="user-info">
                <div className="user-name">
                  {user.username}
                  {user.isBanned && <span className="banned-tag">(已封禁)</span>}
                </div>
                <div className="user-last-message">{conversation.content}</div>
              </div>
              <div className="user-time">
                {formatTime(conversation.timestamp)}
              </div>

            </div>
          );
        })}
      </div>

      <div className="message-area">
        {selectedUser ? (
          <>
            <div className="message-header">
              <div className="message-user-info">
                <div className="user-avatar">
                  {conversations.find(u => u.userInfo.userID === selectedUser)?.userInfo.avatarPath ? (
                    <img src={conversations.find(u => u.userInfo.userID === selectedUser)?.userInfo.avatarPath} alt="头像" />
                  ) : (
                    <img src={`data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWRlLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI3IiByPSI0Ii8+PC9zdmc+`} alt="头像" />
                  )}
                </div>
                <div className="user-name">
                  {conversations.find(u => u.userInfo.userID === selectedUser)?.userInfo.username}
                  {conversations.find(u => u.userInfo.userID === selectedUser)?.userInfo.isBanned && (
                    <span className="banned-tag">(已封禁)</span>
                  )}
                </div>
              </div>
            </div>

            <div className="message-list">
              {messages.map(msg => {
                const isMe = msg.senderID === userInfo.userID;
                return (
                  <div key={msg.messageID} className={`message ${isMe ? 'me' : 'other'}`}>
                    {!isMe && (
                      <div className="message-avatar">
                        <img src={conversations.find(u => u.userInfo.userID === selectedUser)?.userInfo.avatarPath} alt="头像" />
                      </div>
                    )}
                    <div className="message-content">
                      <div className="message-text">{msg.content}</div>
                      <div className="message-time">
                {formatTime(msg.timestamp)}</div>
                    </div>
                    {isMe && (
                      <div className="message-avatar">
                        <img src={userInfo.avatarPath} alt="头像" />
                      </div>
                    )}
                  </div>
                );
              })}
              <div ref={messageEndRef} />
            </div>

            <div className="message-input-container">
              <textarea
                className="message-input"
                placeholder="输入消息..."
                value={messageInput}
                onChange={(e) => setMessageInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    handleSendMessage();
                  }
                }}
                rows={3}
                disabled={conversations.find(u => u.userInfo.userID === selectedUser)?.userInfo.isBanned}
              />
              <button
                className="message-send-btn"
                onClick={handleSendMessage}
                disabled={!messageInput.trim() || !selectedUser ||
                  conversations.find(u => u.userInfo.userID === selectedUser)?.userInfo.isBanned}
              >
                发送
              </button>
            </div>
          </>
        ) : (
          <div className="empty-message">
            <div className="empty-icon">💬</div>
            <h3>选择聊天对象</h3>
            <p>从左侧列表中选择一个对话开始聊天</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default WhisperTab;