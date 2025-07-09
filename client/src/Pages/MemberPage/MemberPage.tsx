
import React, { useCallback, useEffect } from "react";
import { useNavigate, Outlet, useLocation } from "react-router-dom";
import Header from "Components/Header/Header";
import { useUserToken } from "Globals/GlobalStore";
import { useNavigateMain } from "Globals/Navigate";
import "./MemberPage.css";

export const memberPagePath = "/member";

export enum MemberPageTab {
    overview = "overview",
    upload = "upload",
    edit = "edit",
    danmaku = "danmaku",
}

const getTab = (path: string): MemberPageTab => {
    const parts = path.split("/");
    if (parts.length < 3) return MemberPageTab.overview;
    if (parts[2] === "upload") return MemberPageTab.upload;
    return MemberPageTab.overview;
};

export function useNavigateMember() {
    const navigate = useNavigate();

    const navigateMember = useCallback(() => {
        navigate(memberPagePath)
    }, [navigate]);

    const navigateMemberTab = useCallback((tab: MemberPageTab, video_id?: string | number) => {
        if (tab === MemberPageTab.overview || tab === MemberPageTab.upload) {
            navigate(`${memberPagePath}/${tab}`);
            return;
        }
        if ((tab === MemberPageTab.edit || tab === MemberPageTab.danmaku) && video_id) {
            navigate(`${memberPagePath}/${tab}/${video_id}`);
            return;
        }
        navigateMember();
    }, [navigate]);

    return { navigateMember, navigateMemberTab };
}


const MemberPage: React.FC = () => {
    const location = useLocation();
    const userToken = useUserToken();
    const { navigateMemberTab } = useNavigateMember();
    const { navigateMain } = useNavigateMain();

    useEffect(() => {
        console.log("MemberPage mounted or userToken changed:", userToken);
        if (!userToken) {
            console.error("用户未登录，重定向到主页面");
            // 跳转到主页面
            navigateMain();
        }
    }, [userToken, navigateMemberTab]);

    const activeTab = getTab(location.pathname);

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
                        className={`member-sidebar-item ${activeTab === MemberPageTab.upload ? 'active' : ''}`}
                        onClick={() => navigateMemberTab(MemberPageTab.upload)}
                    >
                        上传视频
                    </div>
                    <div
                        className={`member-sidebar-item ${activeTab === MemberPageTab.overview ? 'active' : ''}`}
                        onClick={() => navigateMemberTab(MemberPageTab.overview)}
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
