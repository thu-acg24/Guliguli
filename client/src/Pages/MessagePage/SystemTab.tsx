import React, { useState, useEffect } from 'react';
import { useUserToken } from '../../Globals/GlobalStore';
import { materialAlertError } from '../../Plugins/CommonUtils/Gadgets/AlertGadget';

const SystemTab: React.FC = () => {
  const [notices, setNotices] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const userToken = useUserToken();

  useEffect(() => {
    if (userToken) fetchNotices();
  }, [userToken]);

  const fetchNotices = async () => {
    setLoading(true);
    try {
      setNotices([
        { 
          id: '1', 
          title: '视频审核通过', 
          content: '您的视频已通过审核', 
          time: '1小时前', 
          read: false 
        },
        { 
          id: '2', 
          title: '系统更新', 
          content: '新版本已发布', 
          time: '1天前', 
          read: true 
        }
      ]);
    } catch (error) {
      materialAlertError('加载失败', error.message);
    } finally {
      setLoading(false);
    }
  };

  const markAsRead = (id: string) => {
    setNotices(notices.map(notice => 
      notice.id === id ? { ...notice, read: true } : notice
    ));
  };

  return (
    <div className="system-container">
      <div className="system-header">
        <h3>系统通知</h3>
      </div>
      
      <div className="system-list">
        {notices.map(notice => (
          <div 
            key={notice.id} 
            className={`system-item ${notice.read ? '' : 'unread'}`}
            onClick={() => markAsRead(notice.id)}
          >
            <div className="system-content">
              <div className="system-title">{notice.title}</div>
              <div className="system-text">{notice.content}</div>
              <div className="system-time">{notice.time}</div>
            </div>
            {!notice.read && <div className="unread-badge">新</div>}
          </div>
        ))}
      </div>
    </div>
  );
};

export default SystemTab;