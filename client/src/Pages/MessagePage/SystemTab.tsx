import React, { useState, useEffect } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { QueryNotificationsMessage } from 'Plugins/MessageService/APIs/QueryNotificationsMessage';
import { Notification } from 'Plugins/MessageService/Objects/Notification';
import { formatTime } from 'Components/Formatter';
import './MessagePage.css';
import './SystemTab.css';

const SystemTab: React.FC = () => {
  console.log("load system tab");
  const [notices, setNotices] = useState<Notification[]>([]);
  const [selectedNotice, setSelectedNotice] = useState<Notification | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const userToken = useUserToken();

  useEffect(() => {
    if (userToken) fetchNotices();
  }, [userToken]);

  const fetchNotices = async (): Promise<void> => {
    try {
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
      console.log('Notifications:', notifications);
    } catch (error) {
      console.log('加载通知失败');
      throw new Error('加载通知失败');
    }
  };

  const handleNoticeClick = (notice: Notification) => {
    setSelectedNotice(notice);
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setSelectedNotice(null);
  };

  return (
    <div className="system-container">
      <div className="system-header">
        <h3>系统通知</h3>
      </div>
      <div className="system-list">
        {notices.map((notice, index) => (
          <div
            key={index}
            className={`system-item`}
            onClick={() => handleNoticeClick(notice)}
          >
            <div className="system-content">
              <div className="system-title">{notice.title}</div>
              <div className="system-text">{notice.content}</div>
              <div className="system-time">{formatTime(notice.timestamp)}</div>
            </div>
          </div>
        ))}
      </div>

      {/* 弹窗 */}
      {isModalOpen && selectedNotice && (
        <div className="system-modal-overlay" onClick={closeModal}>
          <div className="system-modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="system-modal-header">
              <h3 className="system-modal-title">{selectedNotice.title}</h3>
              <button className="system-modal-close" onClick={closeModal}>×</button>
            </div>
            <div className="system-modal-body">
              {selectedNotice.content}
            </div>
            <div className="system-modal-footer">
              <div className="system-modal-time">{formatTime(selectedNotice.timestamp)}</div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SystemTab;