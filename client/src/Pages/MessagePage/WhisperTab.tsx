import React, { useState, useEffect, useRef } from 'react';
import { useUserToken } from '../../Globals/GlobalStore';
import { materialAlertError } from '../../Plugins/CommonUtils/Gadgets/AlertGadget';

import { QueryMessagesMessage } from 'Plugins/MessageService/APIs/QueryMessagesMessage';
import { QueryNotificationsMessage } from 'Plugins/MessageService/APIs/QueryNotificationsMessage';
import { QueryReplyNoticesMessage } from 'Plugins/MessageService/APIs/QueryReplyNoticesMessage';
import { SendMessageMessage } from 'Plugins/MessageService/APIs/SendMessageMessage';
import { QueryUserInContactMessage } from 'Plugins/MessageService/APIs/QueryUserInContactMessage';
import { GetUIDByTokenMessage } from 'Plugins/UserService/APIs/GetUIDByTokenMessage';
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";

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
  const [userinfo, setUserInfo] = useState<UserInfo | null>(null);
  // 在组件顶部添加新状态保存刷新前选中的用户
  const [refreshFlag, setRefreshFlag] = useState(false);

  const fetchUserInfo = async (userID: number) => {
    try {
      new QueryUserInfoMessage(userID).send(
        (info: string) => {
          const userInfo = JSON.parse(info);
          setUserInfo(userInfo);
        },
        (e: string) => {
          console.error("获取用户信息失败:", e);
        }
      );
    } catch (e) {
      console.error("获取用户信息异常:", e.message);
    }
  };
  // 添加useEffect处理刷新
  useEffect(() => {
    // 设置延迟确保状态更新完成后再刷新
    fetchConversations();
    if (selectedUser) fetchMessages(selectedUser);
  }, [refreshFlag]);

  // 在初始化时检查是否有保存的选中用户
  useEffect(() => {
    // 从sessionStorage读取保存的选中用户
    const savedUser = sessionStorage.getItem('selectedUser');
    if (savedUser) {
      setSelectedUser(Number(savedUser));
      // 清除存储
      sessionStorage.removeItem('selectedUser');
    }

    // 原有初始化逻辑
    if (userToken) {
      getUserIDByToken(userToken).then(userID => fetchUserInfo(userID)).then(() => {
        fetchConversations();
        fetchNotifications();
        fetchReplyNotices();
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

  async function getUserIDByToken(userToken: string): Promise<number> {
    return new Promise<number>((resolve, reject) => {
      try {
        new GetUIDByTokenMessage(userToken).send(
          (info: string) => {
            const userID = JSON.parse(info);
            resolve(userID);
          },
          (e) => {
            materialAlertError('未找到用户', e);
            reject(new Error('未找到用户'));
          }
        );
      } catch (e) {
        materialAlertError('未找到用户', e);
        reject(new Error('未找到用户'));
      }
    });
  }
  const fetchConversations = async () => {
    setLoading(true);
    try {
      await new Promise<void>((resolve, reject) => {
        new QueryUserInContactMessage(userToken).send(
          (info: string) => {
            try {
              const data = JSON.parse(info);
              setConversations(data);
              resolve(); // 只有这里才表示真正完成
            } catch (e) {
              reject(e);
            }
          },
          (e: string) => reject(new Error(e)) // 将失败转为 rejection
        );
      });
    } catch (error) {
      materialAlertError('加载对话列表失败', error.message);
    } finally {
      setLoading(false); // 此时确保所有操作已完成
    }
  };
  const fetchMessages = async (userID: number): Promise<void> => {
    setLoading(true);

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

      // 处理并设置消息状态
      setMessages(messages.map(msg => ({
        ...msg,
        timestamp: new Date(msg.timestamp).toLocaleTimeString([], {
          hour: '2-digit',
          minute: '2-digit'
        })
      })));

    } catch (error) {
      materialAlertError('加载消息失败', error.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchNotifications = async (): Promise<void> => {
    try {
      // 将回调式API转换为Promise
      const notifications = await new Promise<any>((resolve, reject) => {
        new QueryNotificationsMessage(userToken).send(
          (info: string) => {
            try {
              const data = JSON.parse(info);
              resolve(data); // 成功时返回解析的数据
            } catch (e) {
              reject(new Error("通知解析失败")); // JSON解析错误
            }
          },
          (e: string) => {
            reject(new Error(e)); // 网络或业务错误
          }
        );
      });

      // 处理通知数据
      console.log('Notifications:', notifications);

    } catch (error) {
      console.error('加载通知失败', error instanceof Error ? error.message : String(error));
    }
  };

  const fetchReplyNotices = async (): Promise<ReplyNotice[]> => {
    try {
      // 将回调API转换为Promise
      const notices = await new Promise<ReplyNotice[]>((resolve, reject) => {
        new QueryReplyNoticesMessage(userToken).send(
          (info: string) => {
            try {
              const data: ReplyNotice[] = JSON.parse(info);
              resolve(data);
            } catch (e) {
              reject(new Error(`回复通知解析失败: ${e instanceof Error ? e.message : String(e)}`));
            }
          },
          (e: string) => {
            reject(new Error(`API请求失败: ${e}`));
          }
        );
      });

      console.log('Reply Notices:', notices);
      return notices;

    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : '未知错误';
      console.error('加载回复通知失败:', errorMsg);
      throw error; // 保持错误传播
    }
  };

  // 修改handleSendMessage函数
  const handleSendMessage = async (): Promise<void> => {
    // 1. 输入验证
    if (!messageInput.trim() || !selectedUser) return;

    try {
      // 2. 保存选中的用户ID
      sessionStorage.setItem('selectedUser', selectedUser.toString());

      // 3. 发送消息（Promise化）
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

      // 4. 发送成功后的处理
      setRefreshFlag(prev => !prev); // 使用函数式更新确保最新状态
      setMessageInput("");

    } catch (error) {
      // 5. 错误处理
      const errorMessage = error instanceof Error ? error.message : '发送消息失败';
      materialAlertError('发送消息失败', errorMessage);

      // 可选：恢复输入框内容
      // setMessageInput(messageInput);
    }
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
                const isMe = msg.senderID === userinfo.userID;
                return (
                  <div key={msg.messageID} className={`message ${isMe ? 'me' : 'other'}`}>
                    {!isMe && (
                      <div className="message-avatar">
                        <img src={conversations.find(u => u.userInfo.userID === selectedUser)?.userInfo.avatarPath} alt="头像" />
                      </div>
                    )}
                    <div className="message-content">
                      <div className="message-text">{msg.content}</div>
                      <div className="message-time">{msg.timestamp}</div>
                    </div>
                    {isMe && (
                      <div className="message-avatar">
                        <img src={userinfo.avatarPath} alt="头像" />
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