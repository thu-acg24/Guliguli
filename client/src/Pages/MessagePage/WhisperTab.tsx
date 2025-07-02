import React, { useState, useEffect, useRef } from 'react';
import { useUserToken } from '../../Globals/GlobalStore';
import { materialAlertError } from '../../Plugins/CommonUtils/Gadgets/AlertGadget';

const WhisperTab: React.FC = () => {
  const [conversations, setConversations] = useState<any[]>([]);
  const [selectedUser, setSelectedUser] = useState<string | null>(null);
  const [messages, setMessages] = useState<any[]>([]);
  const [messageInput, setMessageInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messageEndRef = useRef<HTMLDivElement>(null);
  const userToken = useUserToken();

  useEffect(() => {
    if (userToken) fetchConversations();
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
      setConversations([
        { id: '1', name: '技术小助手', lastMessage: '您的问题已解决', lastTime: '10:30', unread: 3 },
        { id: '2', name: '视频审核员', lastMessage: '您的视频已通过审核', lastTime: '昨天', unread: 0 }
      ]);
    } catch (error) {
      materialAlertError('加载对话失败', error.message);
    } finally {
      setLoading(false);
    }
  };

  const fetchMessages = async (userId: string) => {
    setLoading(true);
    try {
      setMessages([
        { id: '1', sender: userId, content: '您好，有什么可以帮您的？', time: '10:30', isMe: false },
        { id: '2', sender: 'me', content: '我的账号遇到登录问题', time: '10:31', isMe: true }
      ]);
    } catch (error) {
      materialAlertError('加载消息失败', error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSendMessage = () => {
    if (!messageInput.trim()) return;
    
    const newMessage = {
      id: Date.now().toString(),
      sender: 'me',
      content: messageInput,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isMe: true
    };
    
    setMessages([...messages, newMessage]);
    setMessageInput('');
    
    setTimeout(() => {
      const replyMessage = {
        id: Date.now().toString() + 'r',
        sender: selectedUser,
        content: ['好的', '明白了', '谢谢'][Math.floor(Math.random()*3)],
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        isMe: false
      };
      setMessages(prev => [...prev, replyMessage]);
    }, 1000);
  };

  return (
    <div className="whisper-container">
      <div className="user-list">
        <div className="user-list-header">
          <h3>我的消息</h3>
          <button className="new-chat-btn">新建聊天</button>
        </div>
        
        {conversations.map(user => (
          <div 
            key={user.id}
            className={`user-item ${selectedUser === user.id ? 'active' : ''}`}
            onClick={() => setSelectedUser(user.id)}
          >
            <div className="user-avatar">
              <img src={`data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWRlLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI3IiByPSI0Ii8+PC9zdmc+`} alt="头像" />
            </div>
            <div className="user-info">
              <div className="user-name">{user.name}</div>
              <div className="user-last-message">{user.lastMessage}</div>
            </div>
            <div className="user-time">{user.lastTime}</div>
          </div>
        ))}
      </div>
      
      <div className="message-area">
        {selectedUser ? (
          <>
            <div className="message-header">
              <div className="message-user-info">
                <div className="user-avatar">
                  <img src={`data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWRlLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI3IiByPSI0Ii8+PC9zdmc+`} alt="头像" />
                </div>
                <div className="user-name">
                  {conversations.find(u => u.id === selectedUser)?.name}
                </div>
              </div>
            </div>
            
            <div className="message-list">
              {messages.map(msg => (
                <div key={msg.id} className={`message ${msg.isMe ? 'me' : 'other'}`}>
                  {!msg.isMe && (
                    <div className="message-avatar">
                      <img src={`data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJub25lIiBzdHJva2U9ImN1cnJlbnRDb2xvciIgc3Ryb2tlLXdpZHRoPSIyIiBzdHJva2UtbGluZWNhcD0icm91bmQiIHN0cm9rZS1saW5lam9pbj0icm91bmQiIGNsYXNzPSJsdWNpZGUgbHVjaWRlLXVzZXIiPjxwYXRoIGQ9Ik0xOSAyMXYtMmE0IDQgMCAwIDAtNC00SDlhNCA0IDAgMCAwLTQgNHYyIi8+PGNpcmNsZSBjeD0iMTIiIGN5PSI3IiByPSI0Ii8+PC9zdmc+`} alt="头像" />
                    </div>
                  )}
                  <div className="message-content">
                    <div className="message-text">{msg.content}</div>
                    <div className="message-time">{msg.time}</div>
                  </div>
                </div>
              ))}
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
              />
              <button 
                className="message-send-btn"
                onClick={handleSendMessage}
                disabled={!messageInput.trim()}
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