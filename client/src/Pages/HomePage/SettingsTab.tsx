// src/Pages/HomePage/SettingsTab.tsx
import React from "react";
import { useOutletContext } from "react-router-dom";
import UserInfoForm from "./SettingsTab/UserInfoForm";
import PasswordForm from "./SettingsTab/PasswordForm";
import "./HomePage.css";

const SettingsTab: React.FC<{ userInfo?: any }> = (props) => {
    const outlet = useOutletContext<{ userInfo: any, refreshUserInfo?: () => void }>();
    const userInfo = props.userInfo ?? outlet?.userInfo;
    const refreshHomePageUserInfo = outlet?.refreshUserInfo;

    return (
        <div className="home-settings-tab">
            <UserInfoForm
                userInfo={userInfo}
                refreshHomePageUserInfo={refreshHomePageUserInfo}
            />
            <PasswordForm />
        </div>
    );
};

export default SettingsTab;