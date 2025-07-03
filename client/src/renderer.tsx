import React from 'react';
import { createRoot } from 'react-dom/client';
import { HashRouter, Route, Routes } from 'react-router-dom';
import MainPage, { mainPagePath } from "Pages/MainPage/MainPage";
import './Styles/index.css';
import VideoPage, { videoPagePath } from 'Pages/VideoPage/VideoPage';
import { AlertGadget } from 'Plugins/CommonUtils/Gadgets/AlertGadget';
import MessagePage, { messagePagePath } from 'Pages/MessagePage/MessagePage';
import WhisperTab from 'Pages/MessagePage/WhisperTab';
import ReplyTab from 'Pages/MessagePage/ReplyTab';
import SystemTab from 'Pages/MessagePage/SystemTab';

const Layout = () => {
    return (
        <>
            <HashRouter>Main
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
                    {/* <Route path={homePagePath} element={<HomePage />} />
                    <Route path={messagePagePath} element={<MessagePage />} />
                    <Route path={managementPagePath} element={<ManagementPage />} />
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