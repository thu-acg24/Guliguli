import React from 'react';
import { createRoot } from 'react-dom/client';
import { HashRouter, Route, Routes } from 'react-router-dom';
import MainPage, { mainPagePath } from "Pages/MainPage";
import './Styles/index.css';
import MessagePage, { messagePagePath } from 'Pages/MessagePage';
import VideoPage, { videoPagePath } from 'Pages/VideoPage';
import { AlertGadget } from 'Plugins/CommonUtils/Gadgets/AlertGadget';
import WhisperTab from 'Pages/MessagePage/WhisperTab';
import ReplyTab from 'Pages/MessagePage/ReplyTab';
import SystemTab from 'Pages/MessagePage/SystemTab';

const Layout = () => {
  return (
    <>
      <HashRouter>
        <Routes>
          <Route path="/" element={<MessagePage />}>
            <Route index element={<WhisperTab />} />
            <Route path="whisper" element={<WhisperTab />} />
            <Route path="reply" element={<ReplyTab />} />
            <Route path="system" element={<SystemTab />} />
          </Route>
          <Route path={messagePagePath} element={<MessagePage />}>
            <Route index element={<WhisperTab />} />
            <Route path="whisper" element={<WhisperTab />} />
            <Route path="reply" element={<ReplyTab />} />
            <Route path="system" element={<SystemTab />} />
          </Route>
          
          <Route path={mainPagePath} element={<MainPage />} />
          <Route path={videoPagePath} element={<VideoPage />} />
        </Routes>
      </HashRouter>
      <AlertGadget />
    </>
  );
};

const container = document.getElementById('root');
const root = createRoot(container);
root.render(<Layout />);