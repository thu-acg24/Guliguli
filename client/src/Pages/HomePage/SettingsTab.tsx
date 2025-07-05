// src/Pages/HomePage/SettingsTab.tsx
import React from "react";
import { useOutletContext } from "react-router-dom";
import { useUserToken } from "Globals/GlobalStore";
import { useRefreshUserInfo } from "Globals/UserHooks";
import UserInfoForm from "./SettingsTab/UserInfoForm";
import PasswordForm from "./SettingsTab/PasswordForm";
import "./HomePage.css";

const SettingsTab: React.FC<{ userInfo?: any }> = (props) => {
    const outlet = useOutletContext<{ userInfo: any, refreshUserInfo?: () => void }>();
    const userInfo = props.userInfo ?? outlet?.userInfo;
    const refreshHomePageUserInfo = outlet?.refreshUserInfo;
    const userToken = useUserToken();
    const refreshUserInfo = useRefreshUserInfo();

    return (
        <div className="home-settings-tab">
            <UserInfoForm
                userInfo={userInfo}
                userToken={userToken}
                refreshUserInfo={refreshUserInfo}
                refreshHomePageUserInfo={refreshHomePageUserInfo}
            />
            <PasswordForm
                userToken={userToken}
            />
        </div>
    );
};

export default SettingsTab;