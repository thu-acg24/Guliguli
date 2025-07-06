import React, { useEffect } from "react";
import { useNavigate, Outlet, useLocation } from "react-router-dom";
import Header from "Components/Header/Header";
import { useUserToken } from "Globals/GlobalStore";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { mainPagePath } from "Pages/MainPage/MainPage";
import "./MemberPage.css";

export const memberPagePath = "/member";

const MemberPage: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const userToken = useUserToken();

    useEffect(() => {
        console.log("MemberPage mounted or userToken changed:", userToken);
        if (!userToken) {
            materialAlertError("请先登录", "您需要登录才能访问创作者中心", () => {
                navigate(mainPagePath);
            });
        }
    }, [userToken, navigate]);

    const getActiveTab = () => {
        const pathParts = location.pathname.split('/');
        if (pathParts.length === 2) return 'overview';
        return pathParts[pathParts.length - 1] || 'overview';
    };

    const activeTab = getActiveTab();

    // 如果用户未登录，不显示内容
    if (!userToken) {
        return (
            <div className="member-page">
                <Header />
                <div className="member-login-message">请先登录后访问创作者中心</div>
            </div>
        );
    }

    return (
        <div className="member-page">
            <Header />
            <div className="member-container">
                <div className="member-sidebar">
                    <div className="member-sidebar-title">创作者中心</div>
                    <div
                        className={`member-sidebar-item ${activeTab === 'upload' ? 'active' : ''}`}
                        onClick={() => navigate(`${memberPagePath}/upload`)}
                    >
                        上传视频
                    </div>
                    <div
                        className={`member-sidebar-item ${activeTab === 'overview' ? 'active' : ''}`}
                        onClick={() => navigate(memberPagePath)}
                    >
                        内容管理
                    </div>
                </div>
                <div className="member-main-content">
                    <Outlet />
                </div>
            </div>
        </div>
    );
};

export default MemberPage;
