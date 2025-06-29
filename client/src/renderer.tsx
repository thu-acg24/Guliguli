import React from 'react'
import { createRoot } from 'react-dom/client';
import { HashRouter, Route, Routes } from 'react-router-dom'
// import LoginPage, {loginPagePath} from "Pages/LoginPage";
import MainPage, { mainPagePath } from "Pages/MainPage";
// import RegisterPage, {registerPagePath} from "Pages/RegisterPage";
// import BookPage, {bookPagePath} from "Pages/BookPage";

const Layout = () => {
    return (
        <HashRouter>Main
            <Routes>
                <Route path="/" element={<MainPage />} />
                {/* <Route path={registerPagePath} element={<RegisterPage />} /> */}
                {/* <Route path={loginPagePath} element={<LoginPage />} />
                <Route path={bookPagePath} element={<BookPage />} /> */}
                <Route path={mainPagePath} element={<MainPage />} />
            </Routes>
        </HashRouter>
    )
}
const container = document.getElementById('root');
const root = createRoot(container);
root.render(<Layout />);
