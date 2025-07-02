import React, { useEffect } from "react";
import { useNavigate, Outlet, useLocation } from "react-router-dom";
import Header from "../Components/Header";
import { useUserToken } from "../Globals/GlobalStore";
import { materialAlertError } from "../Plugins/CommonUtils/Gadgets/AlertGadget";
import { mainPagePath } from "../Pages/MainPage";

export const messagePagePath = "/message";

const MessagePage: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const userToken = useUserToken();

  useEffect(() => {
    if (!userToken) {
      materialAlertError("请先登录", "您需要登录才能查看消息", () => {
        navigate(mainPagePath);
      });
    }
  }, [userToken, navigate]);

  const getActiveTab = () => {
    const pathParts = location.pathname.split('/');
    return pathParts[pathParts.length - 1] || 'whisper';
  };

  const activeTab = getActiveTab();

  return (
    <div className="message-page">
      <Header />
      <div className="message-container">
        <div className="message-sidebar">
          <div
            className={`sidebar-item ${activeTab === 'whisper' ? 'active' : ''}`}
            onClick={() => navigate(`${messagePagePath}/whisper`)}
          >
            我的消息
          </div>
          <div
            className={`sidebar-item ${activeTab === 'reply' ? 'active' : ''}`}
            onClick={() => navigate(`${messagePagePath}/reply`)}
          >
            回复我的
          </div>
          <div
            className={`sidebar-item ${activeTab === 'system' ? 'active' : ''}`}
            onClick={() => navigate(`${messagePagePath}/system`)}
          >
            系统通知
          </div>
        </div>
        <div className="message-content">
          <Outlet />
        </div>
      </div>
    </div>
  );
};

export default MessagePage;