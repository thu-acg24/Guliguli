// src/Pages/HomePage/SettingsTab.tsx
import React, { useState } from "react";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { useOutletContext } from "react-router-dom";
import "./HomePage.css";

interface SettingsTabProps {
    userInfo: UserInfo;
}

const SettingsTab: React.FC<{ userInfo?: any }> = (props) => {
    const outlet = useOutletContext<{ userInfo: any }>();
    const userInfo = props.userInfo ?? outlet?.userInfo;

    const [formData, setFormData] = useState({
        username: userInfo.username,
        avatar: userInfo.avatarPath,
        password: "",
        confirmPassword: ""
    });
    const [saving, setSaving] = useState(false);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);

        // API调用留空
        // 保存设置逻辑
        setTimeout(() => {
            setSaving(false);
            alert("设置已保存");
        }, 1000);
    };

    return (
        <div className="home-settings-tab">
            <form className="home-settings-form" onSubmit={handleSubmit}>
                <div className="home-form-group">
                    <label htmlFor="username">用户名</label>
                    <input
                        type="text"
                        id="username"
                        name="username"
                        value={formData.username}
                        onChange={handleChange}
                    />
                </div>

                <div className="home-form-group">
                    <label>头像</label>
                    <div className="home-avatar-upload">
                        <img
                            src={formData.avatar}
                            alt="当前头像"
                            className="home-current-avatar"
                        />
                        <button
                            type="button"
                            className="home-upload-btn"
                            onClick={() => alert("头像上传功能")}
                        >
                            上传新头像
                        </button>
                    </div>
                </div>

                <div className="home-form-group">
                    <label htmlFor="password">修改密码</label>
                    <input
                        type="password"
                        id="password"
                        name="password"
                        value={formData.password}
                        onChange={handleChange}
                        placeholder="输入新密码"
                    />
                </div>

                <div className="home-form-group">
                    <label htmlFor="confirmPassword">确认密码</label>
                    <input
                        type="password"
                        id="confirmPassword"
                        name="confirmPassword"
                        value={formData.confirmPassword}
                        onChange={handleChange}
                        placeholder="再次输入新密码"
                    />
                </div>

                <button
                    type="submit"
                    className="home-save-btn"
                    disabled={saving}
                >
                    {saving ? "保存中..." : "保存设置"}
                </button>
            </form>
        </div>
    );
};

export default SettingsTab;