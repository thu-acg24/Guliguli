import React, { useState, useEffect, useRef } from 'react';
import { useNavigate,useParams  } from "react-router-dom";
import { useUserToken } from 'Globals/GlobalStore';
import { materialAlertError } from 'Plugins/CommonUtils/Gadgets/AlertGadget';

import { QueryMessagesMessage } from 'Plugins/MessageService/APIs/QueryMessagesMessage';
import { SendMessageMessage } from 'Plugins/MessageService/APIs/SendMessageMessage';
import { QueryUserInContactMessage } from 'Plugins/MessageService/APIs/QueryUserInContactMessage';
import { Message } from 'Plugins/MessageService/Objects/Message';
import { UserInfoWithMessage } from 'Plugins/MessageService/Objects/UserInfoWithMessage';
import { useUserInfo } from 'Globals/GlobalStore';
import { formatTime } from 'Components/GetTime';
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo'
import { fetchOtherUserInfo } from 'Globals/UserService';
import "./MessagePage.css";

export const WhisperTabpath = "/message/whisper";

const WhisperTab: React.FC = () => {
  const { userid } = useParams<{ userid: string | null }>();
  const [conversations, setConversations] = useState<UserInfoWithMessage[]>([]);
  const [selectedUser, setSelectedUser] = useState<number | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageInput, setMessageInput] = useState('');
  const [whispertoUserinfo, setWhispertoUserinfo] = useState<UserInfo|null>(null);
  const messageEndRef = useRef<HTMLDivElement>(null);
  const userToken = useUserToken();
  const navigate = useNavigate();
  // 在组件顶部添加新状态保存刷新前选中的用户
  const [refreshFlag, setRefreshFlag] = useState(false);
  const { userInfo } = useUserInfo();


  // 添加useEffect处理刷新
  useEffect(()=>{
    if(userid){
      setSelectedUser(Number(userid));
      navigate(WhisperTabpath);
    } else setSelectedUser(null);
  },[userid]);

  useEffect(() => {
    fetchConversations();
    if (selectedUser){
      fetchMessages(selectedUser);
      fetchOtherUserInfo(selectedUser).then(setWhispertoUserinfo);
    }else{
      setWhispertoUserinfo(null);
    }
  }, [refreshFlag,selectedUser]);
  
  useEffect(() => {
    console.log("WhisperTab mounted or userToken changed:", userToken);
    if (userToken) {
      fetchConversations();
    } else {
      // 用户登出时清空所有消息相关状态

    }
    setConversations([]);
    setMessages([]);
    setSelectedUser(null);
    setMessageInput('');
  }, [userToken]);

  useEffect(() => {
    if (selectedUser) {
      const loadData = async () => {
        if (selectedUser) {
          await fetchMessages(selectedUser).then(()=>fetchConversations()); // 等待完成
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

    // 跳转函数
  const handleAvatarClick = async (userID:number) => {
        navigate(`/home/${userID}`);
  }
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
        {(selectedUser && whispertoUserinfo) ? (
          <>
            <div className="message-header">
              <div className="message-user-info">
                <div className="user-avatar" >
                    <img src={whispertoUserinfo.avatarPath} alt="头像" onClick={() => handleAvatarClick(whispertoUserinfo.userID)}/>
                </div>
                <div className="user-name" onClick={() => handleAvatarClick(whispertoUserinfo.userID)}>
                  {whispertoUserinfo.username }
                  {whispertoUserinfo.isBanned && (
                    <span className="banned-tag">(已封禁)</span>
                  )}
                </div>
              </div>
            </div>


            <div className="message-list">
              {messages.map(msg => {
                const isMe = userInfo ? msg.senderID === userInfo.userID : false;
                return (
                  <div key={msg.messageID} className={`message ${isMe ? 'me' : 'other'}`}  >
                    {!isMe && (
                      <div className="message-avatar" onClick={() => handleAvatarClick(msg.senderID)}>
                        <img src={whispertoUserinfo.avatarPath} alt="头像" />
                      </div>
                    )}
                    <div className="message-content">
                      <div className="message-text">{msg.content}</div>
                      <div className="message-time">
                        {formatTime(msg.timestamp)}</div>
                    </div>
                    {isMe && (
                      <div className="message-avatar" onClick={() => handleAvatarClick(userInfo.userID)}>
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
                disabled={whispertoUserinfo.isBanned}
              />
              <button
                className="message-send-btn"
                onClick={handleSendMessage}
                disabled={!messageInput.trim() || !selectedUser ||
                  whispertoUserinfo.isBanned}
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