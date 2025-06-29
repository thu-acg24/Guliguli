import React from 'react'
import { createRoot } from 'react-dom/client';
import { HashRouter, Route, Routes } from 'react-router-dom'
import LoginPage, {loginPagePath} from "Pages/LoginPage";
import RegisterPage, {registerPagePath} from "Pages/RegisterPage";

const Layout = () => {
    return (
        <HashRouter>
            <Routes>
                <Route path="/" element={<LoginPage />} />
                <Route path={registerPagePath} element={<RegisterPage />} />
                <Route path={loginPagePath} element={<LoginPage />} />
            </Routes>
        </HashRouter>
    )
}
const container = document.getElementById('root');
const root = createRoot(container);
root.render(<Layout />);
