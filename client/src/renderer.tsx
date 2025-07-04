import React from 'react';
import { createRoot } from 'react-dom/client';
import { HashRouter, Route, Routes } from 'react-router-dom';
import { AlertGadget } from 'Plugins/CommonUtils/Gadgets/AlertGadget';
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

const Layout = () => {
    return (
        <>
            <HashRouter>
                <Routes>
                    <Route path="/" element={<MainPage />} />
                    <Route path={mainPagePath} element={<MainPage />} />
                    <Route path={videoPagePath} element={<VideoPage />} />
                    <Route path={messagePagePath} element={<MessagePage />}>
                        <Route index element={<WhisperTab />} />
                        <Route path="whisper" element={<WhisperTab />} />
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
                    {/*<Route path={managementPagePath} element={<ManagementPage />} />
                    <Route path={searchPagePath} element={<SearchPage />} />
                    <Route path={memberPagePath} element={<MemberPage />} /> */}
                </Routes>
            </HashRouter>
            <AlertGadget />
        </>
    )
}
const container = document.getElementById('root');
const root = createRoot(container);
root.render(<Layout />);