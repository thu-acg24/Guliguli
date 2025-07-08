
import React, { useEffect, useCallback } from "react";
import { useNavigate, Outlet, useLocation } from "react-router-dom";
import Header from "Components/Header/Header";
import { useUserRole } from "Hooks/useUserRole";
import { useNavigateMain } from "Globals/Navigate";
import "./AuditPage.css";

// 审核页面路径常量
export const auditPagePath = "/audit";

// 定义审核页面标签枚举
export enum AuditPageTab {
    video = "video",
    reportVideo = "report-video",
    reportDanmaku = "report-danmaku",
    reportComment = "report-comment",
}

// 根据路径获取当前标签
const getAuditTab = (path: string): AuditPageTab => {
    const parts = path.split("/");
    if (parts.length < 3 || parts[2] === "video") return AuditPageTab.video;
    if (parts[2] === "report") {
        switch (parts[3]) {
            case "video":
                return AuditPageTab.reportVideo;
            case "danmaku":
                return AuditPageTab.reportDanmaku;
            case "comment":
                return AuditPageTab.reportComment;
            default:
                return AuditPageTab.video;
        }
    }
    return AuditPageTab.video;
};

export function useNavigateAudit() {
    const navigate = useNavigate();

    const navigateAudit = useCallback(() => {
        navigate(auditPagePath + "/video");
    }, [navigate]);

    const navigateAuditTab = useCallback((tab: AuditPageTab) => {
        switch (tab) {
            case AuditPageTab.video:
                navigate(`${auditPagePath}/video`);
                break;
            case AuditPageTab.reportVideo:
                navigate(`${auditPagePath}/report/video`);
                break;
            case AuditPageTab.reportDanmaku:
                navigate(`${auditPagePath}/report/danmaku`);
                break;
            case AuditPageTab.reportComment:
                navigate(`${auditPagePath}/report/comment`);
                break;
            default:
                navigate(`${auditPagePath}/video`);
        }
    }, [navigate]);

    return { navigateAudit, navigateAuditTab };
}

const AuditPage: React.FC = () => {
    const { navigateMain } = useNavigateMain();
    const location = useLocation();
    const { loading, error, isAuditor } = useUserRole();
    const { navigateAuditTab } = useNavigateAudit();

    useEffect(() => {
        if (loading) return;
        if (error || !isAuditor) {
            // 用 useNavigateMain 封装跳转主页面
            navigateMain();
        }
    }, [loading, error, isAuditor]);

    const activeTab = getAuditTab(location.pathname);

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
                        className={`audit-sidebar-item ${activeTab === AuditPageTab.video ? 'active' : ''}`}
                        onClick={() => navigateAuditTab(AuditPageTab.video)}
                    >
                        视频审核
                    </div>
                    <div
                        className={`audit-sidebar-item ${activeTab === AuditPageTab.reportVideo ? 'active' : ''}`}
                        onClick={() => navigateAuditTab(AuditPageTab.reportVideo)}
                    >
                        视频举报管理
                    </div>
                    <div
                        className={`audit-sidebar-item ${activeTab === AuditPageTab.reportDanmaku ? 'active' : ''}`}
                        onClick={() => navigateAuditTab(AuditPageTab.reportDanmaku)}
                    >
                        弹幕举报管理
                    </div>
                    <div
                        className={`audit-sidebar-item ${activeTab === AuditPageTab.reportComment ? 'active' : ''}`}
                        onClick={() => navigateAuditTab(AuditPageTab.reportComment)}
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
