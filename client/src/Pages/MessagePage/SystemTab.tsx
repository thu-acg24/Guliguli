import React, { useState, useEffect } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { QueryNotificationsMessage } from 'Plugins/MessageService/APIs/QueryNotificationsMessage';
import{ Notification } from 'Plugins/MessageService/Objects/Notification';
import { formatTime } from 'Components/GetTime';
import './SystemTab.css'; // 确保有对应的CSS文件
const SystemTab: React.FC = () => {
  const [notices, setNotices] = useState<Notification[]>([]);
  const userToken = useUserToken();

  useEffect(() => {
    if (userToken) fetchNotices();
  }, [userToken]);
  
    const fetchNotices = async (): Promise<void> => {
      try {
        // 将回调式API转换为Promise
        const notifications = await new Promise<Notification[]>((resolve, reject) => {
          new QueryNotificationsMessage(userToken).send(
            (info: string) => {
              try {
                const data = JSON.parse(info);
                setNotices(data); 
              } catch (e) {
                reject(new Error("通知解析失败")); 
              }
            },
            (e: string) => {
              reject(new Error(e)); 
            }
          );
        });
        console.log('Notifgications:', notifications);
      } catch (error) {
        console.log('加载通知失败');
        throw new Error('加载通知失败'); 
      }
    };

  return (
    <div className="system-container">
      <div className="system-header">
        <h3>系统通知</h3>
      </div>
      <div className="system-list">
        {notices.map(notice => (
            <div className="system-content">
              <div className="system-title">{notice.title}</div>
              <div className="system-text">{notice.content}</div>
              <div className="system-time">{formatTime(notice.timestamp)}</div>
            </div>
        ))}
      </div>
    </div>
  );
};

export default SystemTab;