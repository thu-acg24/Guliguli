import React from 'react';
import { createRoot } from 'react-dom/client';
import { HashRouter, Route, Routes } from 'react-router-dom';
import { AlertGadget } from 'Plugins/CommonUtils/Gadgets/AlertGadget';
import { useGlobalUserEffects } from 'Globals/UserHooks';
import { useDevTools } from 'Globals/useDevTools';
import './Styles/index.css';
import MainPage, { mainPagePath } from "Pages/MainPage/MainPage";
import VideoPage, { videoPagePath } from 'Pages/VideoPage/VideoPage';
import MessagePage, { messagePagePath } from 'Pages/MessagePage/MessagePage';
import WhisperTab from 'Pages/MessagePage/WhisperTab';
import ReplyTab from 'Pages/MessagePage/ReplyTab';
import SystemTab from 'Pages/MessagePage/SystemTab';
import HomePage, { homePagePath } from 'Pages/HomePage/HomePage';
import VideoTab from 'Pages/HomePage/VideoTab';
import FollowingTab from 'Pages/HomePage/FollowingTab';
import FollowersTab from 'Pages/HomePage/FollowersTab';
import FavoritesTab from 'Pages/HomePage/FavoritesTab';
import HistoryTab from 'Pages/HomePage/HistoryTab';
import SettingsTab from 'Pages/HomePage/SettingsTab';
import MemberPage, { memberPagePath } from 'Pages/MemberPage/MemberPage';
import MemberOverview from 'Pages/MemberPage/MemberOverview';
import MemberUpload from 'Pages/MemberPage/MemberUpload';
import MemberVideoEdit from 'Pages/MemberPage/MemberVideoEdit';
import MemberDanmakuManagement from 'Pages/MemberPage/MemberDanmakuManagement';

// 内部组件，用于在Router内部初始化开发者工具
const RouterContent = () => {
    // 初始化开发者工具（仅在开发环境中生效）
    useDevTools();

    return (
        <Routes>
            <Route path="/" element={<MainPage />} />
            <Route path={mainPagePath} element={<MainPage />} />
            <Route path={videoPagePath} element={<VideoPage />} />
            <Route path={messagePagePath} element={<MessagePage />}>
                <Route index element={<WhisperTab />} />
                <Route path="whisper" element={<WhisperTab />} />
                <Route path={`whisper/:userid`} element={<WhisperTab />} />
                <Route path="reply" element={<ReplyTab />} />
                <Route path="system" element={<SystemTab />} />
            </Route>
            <Route path={homePagePath} element={<HomePage />}>
                <Route index element={<VideoTab />} />
                <Route path="videos" element={<VideoTab />} />
                <Route path="following" element={<FollowingTab />} />
                <Route path="followers" element={<FollowersTab />} />
                <Route path="favorites" element={<FavoritesTab />} />
                <Route path="history" element={<HistoryTab />} />
                <Route path="settings" element={<SettingsTab />} />
            </Route>
            <Route path={memberPagePath} element={<MemberPage />}>
                <Route index element={<MemberOverview />} />
                <Route path="upload" element={<MemberUpload />} />
                <Route path="edit/:videoID" element={<MemberVideoEdit />} />
                <Route path="danmaku/:videoID" element={<MemberDanmakuManagement />} />
            </Route>
            {/*<Route path={managementPagePath} element={<ManagementPage />} />
            <Route path={searchPagePath} element={<SearchPage />} /> */}
        </Routes>
    );
};

const Layout = () => {
    // 初始化全局用户状态的副作用
    useGlobalUserEffects();

    return (
        <>
            <HashRouter>
                <RouterContent />
            </HashRouter>
            <AlertGadget />
        </>
    )
}
const container = document.getElementById('root');
const root = createRoot(container);
root.render(<Layout />);