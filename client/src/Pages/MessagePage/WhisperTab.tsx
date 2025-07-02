import React, { useState, useEffect, useRef } from 'react';
import { useUserToken } from '../../Globals/GlobalStore';
import { materialAlertError } from '../../Plugins/CommonUtils/Gadgets/AlertGadget';

import { QueryMessagesMessage} from 'Plugins/MessageService/APIs/QueryMessagesMessage';
import { QueryNotificationsMessage} from 'Plugins/MessageService/APIs/QueryNotificationsMessage';
import { QueryReplyNoticesMessage} from 'Plugins/MessageService/APIs/QueryReplyNoticesMessage';
import { SendMessageMessage} from 'Plugins/MessageService/APIs/SendMessageMessage';
import { QueryUserInContactMessage} from 'Plugins/MessageService/APIs/QueryUserInContactMessage';

interface UserInfo {
  userID: number;
  username: string;
  avatarPath: string;
  isBanned: boolean;
}

interface Message {
  messageID: number;
  senderID: number;
  content: string;
  timestamp: string;
}

interface Notification {
  notificationID: number;
  content: string;
  timestamp: string;
}

interface ReplyNotice {
  noticeID: number;
  senderID: number;
  content: string;
  commentID: number;
  originalContent: string;
  originalCommentID: number;
  timestamp: string;
}

interface UserInfoWithMessage {
  userInfo: UserInfo;
  unreadCount: number;
  timestamp: string;
  content: string;
}

const WhisperTab: React.FC = () => {
  const [conversations, setConversations] = useState<UserInfoWithMessage[]>([]);
  const [selectedUser, setSelectedUser] = useState<number | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageInput, setMessageInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messageEndRef = useRef<HTMLDivElement>(null);
  const userToken = useUserToken();

  useEffect(() => {
    if (userToken) {
      fetchConversations();
      fetchNotifications();
      fetchReplyNotices();
    }
  }, [userToken]);

  useEffect(() => {
    if (selectedUser) fetchMessages(selectedUser);
  }, [selectedUser]);

  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const fetchConversations = async () => {
    setLoading(true);
    try {
      new QueryUserInContactMessage(userToken).send(
        (info: string) => {
          const data = JSON.parse(info);
          console.log("data type: ", typeof(data));
          setConversations(data);
        },
        (e: string) => {
          materialAlertError('加载对话列表失败', e);
        }
      );
    } catch (error) {
      materialAlertError('加载对话列表失败', error.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchMessages = async (userId: number) => {
    setLoading(true);
    try {
      new QueryMessagesMessage(userToken, userId).send(
        (info: string) => {
          const data = JSON.parse(info);
          setMessages(data.map(msg => ({
            ...msg,
            timestamp: new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
          })));
        },
        (e: string) => {
          materialAlertError('加载消息失败', e);
        }
      );
    } catch (error) {
      materialAlertError('加载消息失败', error.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchNotifications = async () => {
    try {
      new QueryNotificationsMessage(userToken).send(
        (info: string) => {
          // 处理系统通知数据
          const data = JSON.parse(info);
          console.log('Notifications:', data);
        },
        (e: string) => {
          console.error('加载通知失败', e);
        }
      );
    } catch (error) {
      console.error('加载通知失败', error);
    }
  };

  const fetchReplyNotices = async () => {
    try {
      new QueryReplyNoticesMessage(userToken).send(
        (info: string) => {
          // 处理回复通知数据
          const data = JSON.parse(info);
          console.log('Reply Notices:', data);
        },
        (e: string) => {
          console.error('加载回复通知失败', e);
        }
      );
    } catch (error) {
      console.error('加载回复通知失败', error);
    }
  };

  const handleSendMessage = () => {
    if (!messageInput.trim() || !selectedUser) return;
    
    const currentUserId = JSON.parse(userToken).userID;
    const newMessage = {
      messageID: Date.now(),
      senderID: currentUserId,
      content: messageInput,
      timestamp: new Date().toISOString(),
      isMe: true
    };
    
    // 乐观更新
    setMessages(prev => [...prev, {
      ...newMessage,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }]);
    setMessageInput('');
    
    // 更新对话列表的最后一条消息
    setConversations(prev => 
      prev.map(conv => 
        conv.userInfo.userID === selectedUser 
          ? { 
              ...conv, 
              content: messageInput,
              timestamp: new Date().toISOString()
            } 
          : conv
      )
    );
    
    // 实际发送消息
    new SendMessageMessage(userToken, selectedUser, messageInput).send(
      () => {
        // 发送成功，不需要额外处理
      },
      (e: string) => {
        materialAlertError('发送消息失败', e);
        // 回滚乐观更新
        setMessages(prev => prev.filter(msg => msg.messageID !== newMessage.messageID));
        setConversations(prev => 
          prev.map(conv => 
            conv.userInfo.userID === selectedUser 
              ? { 
                  ...conv, 
                  content: prev.find(c => c.userInfo.userID === selectedUser)?.content || '',
                  timestamp: prev.find(c => c.userInfo.userID === selectedUser)?.timestamp || ''
                } 
              : conv
          )
        );
      }
    );
  };

  const formatTime = (timestamp: string) => {
    const now = new Date();
    const date = new Date(timestamp);
    const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));
    
    if (diffDays === 0) {
      return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } else if (diffDays === 1) {
      return '昨天';
    } else if (diffDays < 7) {
      return `${diffDays}天前`;
    } else {
      return date.toLocaleDateString([], { month: 'numeric', day: 'numeric' });
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
                {user.avatarPath ? (
                  <img src={user.avatarPath} alt="头像" />
                ) : (
                  <img src={`data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWRlLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI3IiByPSI0Ii8+PC9zdmc+`} alt="头像" />
                )}
              </div>
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
              {conversation.unreadCount > 0 && (
                <div className="unread-count">{conversation.unreadCount}</div>
              )}
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
                const isMe = msg.senderID === JSON.parse(userToken).userID;
                return (
                  <div key={msg.messageID} className={`message ${isMe ? 'me' : 'other'}`}>
                    {!isMe && (
                      <div className="message-avatar">
                        {conversations.find(u => u.userInfo.userID === selectedUser)?.userInfo.avatarPath ? (
                          <img src={conversations.find(u => u.userInfo.userID === selectedUser)?.userInfo.avatarPath} alt="头像" />
                        ) : (
                          <img src={`data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWRlLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI7IiByPSI0Ii8+PC9zdmc+`} alt="头像" />
                        )}
                      </div>
                    )}
                    <div className="message-content">
                      <div className="message-text">{msg.content}</div>
                      <div className="message-time">{msg.timestamp}</div>
                    </div>
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