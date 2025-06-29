import React from 'react'
import { createRoot } from 'react-dom/client';
import { HashRouter, Route, Routes } from 'react-router-dom'
import MainPage, { mainPagePath } from "Pages/MainPage";
import './Styles/index.css';
// import HomePage, { homePagePath } from 'Pages/HomePage';
// import MessagePage, { messagePagePath } from 'Pages/MessagePage';
// import ManagementPage, { managementPagePath } from 'Pages/ManagementPage';
import VideoPage, { videoPagePath } from 'Pages/VideoPage';
// import SearchPage, { searchPagePath } from 'Pages/SearchPage';
// import MemberPage, { memberPagePath } from 'Pages/MemberPage';


const Layout = () => {
    return (
        <HashRouter>Main
            <Routes>
                <Route path="/" element={<VideoPage />} />
                <Route path={mainPagePath} element={<MainPage />} />
                {<Route path={videoPagePath} element={<VideoPage />} />
                /* <Route path={homePagePath} element={<HomePage />} />
                <Route path={messagePagePath} element={<MessagePage />} />
                <Route path={managementPagePath} element={<ManagementPage />} />
                <Route path={searchPagePath} element={<SearchPage />} />
                <Route path={memberPagePath} element={<MemberPage />} /> */}
            </Routes>
        </HashRouter>
    )
}
const container = document.getElementById('root');
const root = createRoot(container);
root.render(<Layout />);
