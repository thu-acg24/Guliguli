import React, { useEffect, useState } from "react";
import { useNavigate, Outlet, useLocation } from "react-router-dom";
import Header from "Components/Header/Header";
import { materialAlertError } from "Plugins/CommonUtils/Gadgets/AlertGadget";
import { useUserRole } from "Hooks/useUserRole";
import { mainPagePath } from "Pages/MainPage/MainPage";
import "./AuditPage.css";

export const auditPagePath = "/audit";

const AuditPage: React.FC = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const { userRole, loading, error, isAdmin, isAuditor } = useUserRole();
    console.log("!!!!!!!!!", loading)
    console.log("!!!!!!!!!", userRole)
    console.log("!!!!!!!!!", isAuditor)


    useEffect(() => {
        if (loading) return;

        if (error || (!isAuditor)) {
            materialAlertError("加载错误", "您未登录或您不是审核员", () => {
                navigate(mainPagePath);
            });
        }
    }, [loading, error, isAuditor, navigate]);

    const getActiveTab = () => {
        const pathParts = location.pathname.split('/');
        if (pathParts.length === 2 || pathParts[2] === 'video') return 'video';
        if (pathParts[2] === 'report') {
            const reportType = pathParts[3];
            if (reportType === 'video') return 'report-video';
            if (reportType === 'danmaku') return 'report-danmaku';
            if (reportType === 'comment') return 'report-comment';
        }
        return 'video';
    };

    const activeTab = getActiveTab();

    if (loading) {
        return (
            <div className="audit-page">
                <Header />
                <div className="audit-loading">加载中...</div>
            </div>
        );
    }

    if (!isAuditor) {
        return null;
    }

    return (
        <div className="audit-page">
            <Header />
            <div className="audit-container">
                <div className="audit-sidebar">
                    <div className="audit-sidebar-title">审核管理中心</div>
                    <div
                        className={`audit-sidebar-item ${activeTab === 'video' ? 'active' : ''}`}
                        onClick={() => navigate(`${auditPagePath}/video`)}
                    >
                        视频审核
                    </div>
                    <div
                        className={`audit-sidebar-item ${activeTab === 'report-video' ? 'active' : ''}`}
                        onClick={() => navigate(`${auditPagePath}/report/video`)}
                    >
                        视频举报管理
                    </div>
                    <div
                        className={`audit-sidebar-item ${activeTab === 'report-danmaku' ? 'active' : ''}`}
                        onClick={() => navigate(`${auditPagePath}/report/danmaku`)}
                    >
                        弹幕举报管理
                    </div>
                    <div
                        className={`audit-sidebar-item ${activeTab === 'report-comment' ? 'active' : ''}`}
                        onClick={() => navigate(`${auditPagePath}/report/comment`)}
                    >
                        评论举报管理
                    </div>
                </div>
                <div className="audit-main-content">
                    <Outlet />
                </div>
            </div>
        </div>
    );
};

export default AuditPage;
