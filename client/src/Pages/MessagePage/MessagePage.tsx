import React, { useEffect, useCallback } from "react";
import { useNavigate, Outlet, useLocation } from "react-router-dom";
import Header from "Components/Header/Header";
import { useUserToken } from "Globals/GlobalStore";
import { useNavigateMain } from "Globals/Navigate";
import "./MessagePage.css";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { message } from "antd";

// 消息页面路径
export const messagePagePath = "/message";

// 定义消息页面标签枚举
export enum MessagePageTab {
  whisper = "whisper",
  reply = "reply",
  system = "system",
}

// 获取当前 tab
const getMessageTab = (path: string): MessagePageTab => {
  const parts = path.split("/");
  // /message/whisper 结构，parts[2] 是 tab
  if (parts.length < 3) return MessagePageTab.whisper;
  const tab = parts[2];
  return Object.values(MessagePageTab).includes(tab as MessagePageTab) ? (tab as MessagePageTab) : MessagePageTab.whisper;
};

// 导航 hooks
export function useNavigateMessage() {
  const navigate = useNavigate();
  const navigateMessage = useCallback(() => {
    navigate(messagePagePath);
  }, [navigate]);

  const navigateMessageTab = useCallback((tab: MessagePageTab, user_id?: string | number) => {
    if (!user_id) {
      navigate(`${messagePagePath}/${tab}`);
    } else if (tab === MessagePageTab.whisper) {
      console.log("Navigating to whisper tab with user_id:", user_id);
      navigate(`${messagePagePath}/${tab}/${user_id}`);
    } else {
      navigateMessage();
    }
  }, [navigate]);

  return { navigateMessage, navigateMessageTab };
}

const MessagePage: React.FC = () => {
  const location = useLocation();
  const userToken = useUserToken();
  const { navigateMessageTab } = useNavigateMessage();
  const { navigateMain } = useNavigateMain();

  useEffect(() => {
    if (!userToken) {
      // 未登录时跳转到主页面
      console.error("用户未登录，重定向到主页面");
      navigateMain();
    }
  }, [userToken]);

  const activeTab = getMessageTab(location.pathname);

  return (
    <div className="message-page">
      <Header />
      <div className="message-backcontainer">
      <div className="message-container">
        <div className="message-sidebar">
        <div className="sidebar-title">消息中心</div>
          <div
            className={`sidebar-item ${activeTab === MessagePageTab.whisper ? 'active' : ''}`}
            onClick={() => navigateMessageTab(MessagePageTab.whisper)}
          >
            我的消息
          </div>
          <div
            className={`sidebar-item ${activeTab === MessagePageTab.reply ? 'active' : ''}`}
            onClick={() => navigateMessageTab(MessagePageTab.reply)}
          >
            回复我的
          </div>
          <div
            className={`sidebar-item ${activeTab === MessagePageTab.system ? 'active' : ''}`}
            onClick={() => navigateMessageTab(MessagePageTab.system)}
          >
            系统通知
          </div>
        </div>
        <div className="message-content">
          <Outlet />
        </div>
      </div>
      </div>
    </div>
  );
};

export default MessagePage;