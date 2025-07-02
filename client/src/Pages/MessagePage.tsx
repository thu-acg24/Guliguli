// src/Pages/MessagePage.tsx
import React, { useState, useEffect } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import Header from "../Components/Header";
import LoginModal from "../Components/LoginModal";
import { useUserToken } from "../Globals/GlobalStore";

export const messagePagePath = "/message/"

// 定义消息类型
interface Message {
  id: string;
  senderId: string;
  senderName: string;
  senderAvatar: string;
  content: string;
  timestamp: string;
  isOwn: boolean;
}

interface Reply {
  id: string;
  userId: string;
  userName: string;
  userAvatar: string;
  content: string;
  timestamp: string;
  videoId: string;
  videoTitle: string;
  commentId: string;
  isLiked: boolean;
  likeCount: number;
}

interface SystemNotification {
  id: string;
  title: string;
  content: string;
  timestamp: string;
}

// 定义API调用占位符
const fetchWhisperUsers = async (userId: string): Promise<any[]> => {
  console.log(`API调用: 获取用户 ${userId} 的私信联系人列表`);
  // 实际API调用示例:
  // return await fetch(`/api/messages/${userId}/contacts`).then(res => res.json());
  return [
    { id: "2", name: "用户A", avatar: "" },
    { id: "3", name: "用户B", avatar: "" },
    { id: "4", name: "用户C", avatar: "" },
  ];
};

const fetchChatHistory = async (userId: string, contactId: string): Promise<Message[]> => {
  console.log(`API调用: 获取用户 ${userId} 和 ${contactId} 的聊天记录`);
  // 实际API调用示例:
  // return await fetch(`/api/messages/${userId}/history/${contactId}`).then(res => res.json());
  return [
    { id: "1", senderId: userId, senderName: "我", senderAvatar: "", content: "你好！", timestamp: "10:30", isOwn: true },
    { id: "2", senderId: contactId, senderName: "用户A", senderAvatar: "", content: "你好，有什么问题吗？", timestamp: "10:32", isOwn: false },
    { id: "3", senderId: userId, senderName: "我", senderAvatar: "", content: "我想问一下关于视频上传的问题", timestamp: "10:35", isOwn: true },
  ];
};

const sendMessage = async (userId: string, contactId: string, content: string): Promise<boolean> => {
  console.log(`API调用: 用户 ${userId} 发送消息给 ${contactId}: ${content}`);
  // 实际API调用示例:
  // return await fetch(`/api/messages/${userId}/send`, {
  //   method: 'POST',
  //   body: JSON.stringify({ receiverId: contactId, content }),
  //   headers: { 'Content-Type': 'application/json' }
  // }).then(res => res.ok);
  return true;
};

const fetchReplies = async (userId: string): Promise<Reply[]> => {
  console.log(`API调用: 获取用户 ${userId} 的回复`);
  // 实际API调用示例:
  // return await fetch(`/api/messages/${userId}/replies`).then(res => res.json());
  return [
    {
      id: "1",
      userId: "5",
      userName: "用户D",
      userAvatar: "",
      content: "这个视频讲得太好了！",
      timestamp: "2023-06-15 14:30",
      videoId: "v123",
      videoTitle: "React高级教程",
      commentId: "c456",
      isLiked: false,
      likeCount: 5
    },
    {
      id: "2",
      userId: "6",
      userName: "用户E",
      userAvatar: "",
      content: "我不同意你的观点，因为...",
      timestamp: "2023-06-14 09:15",
      videoId: "v456",
      videoTitle: "TypeScript实战",
      commentId: "c789",
      isLiked: true,
      likeCount: 12
    }
  ];
};

const likeReply = async (userId: string, replyId: string): Promise<boolean> => {
  console.log(`API调用: 用户 ${userId} 点赞回复 ${replyId}`);
  // 实际API调用示例:
  // return await fetch(`/api/messages/${userId}/like/${replyId}`, {
  //   method: 'POST'
  // }).then(res => res.ok);
  return true;
};

const sendReply = async (userId: string, replyId: string, content: string): Promise<boolean> => {
  console.log(`API调用: 用户 ${userId} 回复评论 ${replyId}: ${content}`);
  // 实际API调用示例:
  // return await fetch(`/api/messages/${userId}/reply`, {
  //   method: 'POST',
  //   body: JSON.stringify({ replyId, content }),
  //   headers: { 'Content-Type': 'application/json' }
  // }).then(res => res.ok);
  return true;
};

const fetchSystemNotifications = async (userId: string): Promise<SystemNotification[]> => {
  console.log(`API调用: 获取用户 ${userId} 的系统通知`);
  // 实际API调用示例:
  // return await fetch(`/api/messages/${userId}/notifications`).then(res => res.json());
  return [
    {
      id: "1",
      title: "系统维护通知",
      content: "我们将于本周六凌晨2点至4点进行系统维护，届时服务将不可用。",
      timestamp: "2023-06-10 10:00"
    },
    {
      id: "2",
      title: "新功能上线",
      content: "视频剪辑功能已上线，欢迎体验！",
      timestamp: "2023-06-05 15:30"
    }
  ];
};

const MessagePage: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const location = useLocation();
  const navigate = useNavigate();
  const userToken = useUserToken();
  const [showLoginModal, setShowLoginModal] = useState(false);
  const [activeTab, setActiveTab] = useState<'whisper' | 'reply' | 'system'>('whisper');
  const [selectedContact, setSelectedContact] = useState<string | null>(null);
  const [whisperUsers, setWhisperUsers] = useState<any[]>([]);
  const [messages, setMessages] = useState<Message[]>([]);
  const [newMessage, setNewMessage] = useState("");
  const [replies, setReplies] = useState<Reply[]>([]);
  const [notifications, setNotifications] = useState<SystemNotification[]>([]);
  const [replyModal, setReplyModal] = useState<{ open: boolean, replyId?: string }>({ open: false });
  const [replyContent, setReplyContent] = useState("");

  // 检查用户是否登录
  useEffect(() => {
    if (!userToken) {
      setShowLoginModal(true);
    }
  }, [userToken]);

  // 解析URL路径确定当前标签页
  useEffect(() => {
    const pathParts = location.pathname.split('/');
    const tab = pathParts[pathParts.length - 1] as 'whisper' | 'reply' | 'system';

    if (tab && ['whisper', 'reply', 'system'].includes(tab)) {
      setActiveTab(tab);

      // 如果是whisper标签且有联系人ID
      if (tab === 'whisper' && pathParts.length > 4) {
        const contactId = pathParts[pathParts.length - 1];
        setSelectedContact(contactId);
      } else {
        setSelectedContact(null);
      }
    } else {
      // 默认跳转到whisper
      navigate(`/message/${userId}/whisper`, { replace: true });
    }
  }, [location.pathname, userId, navigate]);

  // 加载私信联系人
  useEffect(() => {
    if (activeTab === 'whisper' && userId) {
      fetchWhisperUsers(userId).then(data => {
        setWhisperUsers(data);
      });
    }
  }, [activeTab, userId]);

  // 加载聊天记录
  useEffect(() => {
    if (activeTab === 'whisper' && userId && selectedContact) {
      fetchChatHistory(userId, selectedContact).then(data => {
        setMessages(data);
      });
    } else {
      setMessages([]);
    }
  }, [activeTab, userId, selectedContact]);

  // 加载回复
  useEffect(() => {
    if (activeTab === 'reply' && userId) {
      fetchReplies(userId).then(data => {
        setReplies(data);
      });
    }
  }, [activeTab, userId]);

  // 加载系统通知
  useEffect(() => {
    if (activeTab === 'system' && userId) {
      fetchSystemNotifications(userId).then(data => {
        setNotifications(data);
      });
    }
  }, [activeTab, userId]);

  // 处理发送消息
  const handleSendMessage = () => {
    if (!userId || !selectedContact || !newMessage.trim()) return;

    sendMessage(userId, selectedContact, newMessage).then(success => {
      if (success) {
        // 添加到本地消息列表
        setMessages(prev => [
          ...prev,
          {
            id: Date.now().toString(),
            senderId: userId,
            senderName: "我",
            senderAvatar: "",
            content: newMessage,
            timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            isOwn: true
          }
        ]);
        setNewMessage("");
      }
    });
  };

  // 处理点赞回复
  const handleLikeReply = (replyId: string) => {
    if (!userId) return;

    likeReply(userId, replyId).then(success => {
      if (success) {
        setReplies(prev =>
          prev.map(reply =>
            reply.id === replyId
              ? { ...reply, isLiked: !reply.isLiked, likeCount: reply.isLiked ? reply.likeCount - 1 : reply.likeCount + 1 }
              : reply
          )
        );
      }
    });
  };

  // 处理发送回复
  const handleSendReply = () => {
    if (!userId || !replyModal.replyId || !replyContent.trim()) return;

    sendReply(userId, replyModal.replyId, replyContent).then(success => {
      if (success) {
        setReplyModal({ open: false });
        setReplyContent("");
        alert("回复发送成功！");
      }
    });
  };

  // 处理跳转到视频
  const handleGoToVideo = (videoId: string) => {
    alert(`跳转到视频页面，视频ID: ${videoId}`);
  };

  return (
    <div className="message-page">
      <Header />

      <div className="message-container">
        {/* 侧边栏 */}
        <div className="message-sidebar">
          <div
            className={`sidebar-item ${activeTab === 'whisper' ? 'active' : ''}`}
            onClick={() => navigate(`/message/${userId}/whisper`)}
          >
            我的消息
          </div>
          <div
            className={`sidebar-item ${activeTab === 'reply' ? 'active' : ''}`}
            onClick={() => navigate(`/message/${userId}/reply`)}
          >
            回复我的
          </div>
          <div
            className={`sidebar-item ${activeTab === 'system' ? 'active' : ''}`}
            onClick={() => navigate(`/message/${userId}/system`)}
          >
            系统通知
          </div>
        </div>

        {/* 主内容区 */}
        <div className="message-content">
          {activeTab === 'whisper' && (
            <div className="whisper-container">
              {/* 联系人列表 */}
              <div className="contact-list">
                {whisperUsers.map(user => (
                  <div
                    key={user.id}
                    className={`contact-item ${selectedContact === user.id ? 'active' : ''}`}
                    onClick={() => navigate(`/message/${userId}/whisper/${user.id}`)}
                  >
                    <div className="contact-avatar">
                      {user.avatar ? (
                        <img src={user.avatar} alt={user.name} />
                      ) : (
                        <div className="default-avatar">{user.name.charAt(0)}</div>
                      )}
                    </div>
                    <div className="contact-name">{user.name}</div>
                  </div>
                ))}
              </div>

              {/* 聊天区域 */}
              <div className="chat-area">
                {selectedContact ? (
                  <>
                    <div className="chat-header">
                      <div className="contact-name">
                        {whisperUsers.find(u => u.id === selectedContact)?.name || '未知用户'}
                      </div>
                    </div>

                    <div className="chat-messages">
                      {messages.map(msg => (
                        <div
                          key={msg.id}
                          className={`message ${msg.isOwn ? 'own' : ''}`}
                        >
                          {!msg.isOwn && (
                            <div className="message-avatar">
                              {msg.senderAvatar ? (
                                <img src={msg.senderAvatar} alt={msg.senderName} />
                              ) : (
                                <div className="default-avatar">{msg.senderName.charAt(0)}</div>
                              )}
                            </div>
                          )}

                          <div className="message-content">
                            <div className="message-text">{msg.content}</div>
                            <div className="message-time">{msg.timestamp}</div>
                          </div>

                          {msg.isOwn && (
                            <div className="message-avatar">
                              {msg.senderAvatar ? (
                                <img src={msg.senderAvatar} alt={msg.senderName} />
                              ) : (
                                <div className="default-avatar">{msg.senderName.charAt(0)}</div>
                              )}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>

                    <div className="message-input">
                      <input
                        type="text"
                        value={newMessage}
                        onChange={(e) => setNewMessage(e.target.value)}
                        onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                        placeholder="输入消息..."
                      />
                      <button onClick={handleSendMessage}>发送</button>
                    </div>
                  </>
                ) : (
                  <div className="empty-chat">
                    请从左侧选择一个联系人开始聊天
                  </div>
                )}
              </div>
            </div>
          )}

          {activeTab === 'reply' && (
            <div className="reply-list">
              {replies.map(reply => (
                <div key={reply.id} className="reply-item">
                  <div className="reply-header">
                    <div className="user-info">
                      <div className="user-avatar">
                        {reply.userAvatar ? (
                          <img src={reply.userAvatar} alt={reply.userName} />
                        ) : (
                          <div className="default-avatar">{reply.userName.charAt(0)}</div>
                        )}
                      </div>
                      <div className="user-name">{reply.userName}</div>
                    </div>
                    <div className="reply-time">{reply.timestamp}</div>
                  </div>

                  <div className="reply-content">{reply.content}</div>

                  <div className="reply-actions">
                    <button
                      className={`like-btn ${reply.isLiked ? 'liked' : ''}`}
                      onClick={() => handleLikeReply(reply.id)}
                    >
                      {reply.isLiked ? '已赞' : '点赞'} ({reply.likeCount})
                    </button>
                    <button
                      className="reply-btn"
                      onClick={() => setReplyModal({ open: true, replyId: reply.id })}
                    >
                      回复
                    </button>
                  </div>

                  <div
                    className="original-comment"
                    onClick={() => handleGoToVideo(reply.videoId)}
                  >
                    <div className="video-title">原评论: {reply.videoTitle}</div>
                    <div className="comment-preview">你: {reply.content.substring(0, 50)}...</div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {activeTab === 'system' && (
            <div className="system-list">
              {notifications.map(notification => (
                <div key={notification.id} className="notification-item">
                  <div className="notification-header">
                    <div className="notification-title">{notification.title}</div>
                    <div className="notification-time">{notification.timestamp}</div>
                  </div>
                  <div className="notification-content">{notification.content}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* 登录弹窗 */}
      <LoginModal
        isOpen={showLoginModal}
        onClose={() => setShowLoginModal(false)}
      />

      {/* 回复弹窗 */}
      {replyModal.open && (
        <div className="modal" onClick={() => setReplyModal({ open: false })}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <div className="modal-title">回复评论</div>
              <div className="modal-close" onClick={() => setReplyModal({ open: false })}>&times;</div>
            </div>
            <div className="modal-body">
              <textarea
                className="reply-textarea"
                value={replyContent}
                onChange={(e) => setReplyContent(e.target.value)}
                placeholder="输入回复内容..."
              />
              <div className="modal-actions">
                <button className="cancel-btn" onClick={() => setReplyModal({ open: false })}>取消</button>
                <button className="send-btn" onClick={handleSendReply}>发送</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default MessagePage;