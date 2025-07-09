// src/Pages/HomePage/SettingsTab.tsx
import React from "react";
import { useOutletContext } from "react-router-dom";
import UserInfoForm from "./SettingsTab/UserInfoForm";
import PasswordForm from "./SettingsTab/PasswordForm";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import "./HomePage.css";

const SettingsTab: React.FC = () => {
    const outlet = useOutletContext<{ userInfo: UserInfo, refreshUserInfo: () => void }>();
    const userInfo = outlet.userInfo;
    const refreshHomePageUserInfo = outlet.refreshUserInfo;

    return (
        <>
            <div className="home-tab-title">个人设置</div>
            <div className="home-settings-tab">
                <UserInfoForm
                    userInfo={userInfo}
                    refreshHomePageUserInfo={refreshHomePageUserInfo}
                />
                <PasswordForm />
            </div>
        </>
    );
};

export default SettingsTab;