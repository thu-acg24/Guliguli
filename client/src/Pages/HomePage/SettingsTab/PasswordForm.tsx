// src/Pages/HomePage/SettingsTab/PasswordForm.tsx
import React, { useState } from "react";
import { ModifyPasswordMessage } from "Plugins/UserService/APIs/ModifyPasswordMessage";
import "../HomePage.css";

interface PasswordFormProps {
    userToken: string | null;
}

const PasswordForm: React.FC<PasswordFormProps> = ({ userToken }) => {
    const [passwordData, setPasswordData] = useState({
        oldPassword: "",
        password: "",
        confirmPassword: ""
    });
    const [savingPassword, setSavingPassword] = useState(false);
    const [passwordMessage, setPasswordMessage] = useState("");
    const [isPasswordSuccess, setIsPasswordSuccess] = useState(false);

    const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setPasswordData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handlePasswordSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!userToken) {
            setIsPasswordSuccess(false);
            setPasswordMessage("请先登录");
            return;
        }

        setSavingPassword(true);
        setPasswordMessage("");

        try {
            // 验证密码
            if (!passwordData.oldPassword) {
                setIsPasswordSuccess(false);
                setPasswordMessage("请输入旧密码");
                return;
            }
            if (!passwordData.password) {
                setIsPasswordSuccess(false);
                setPasswordMessage("请输入新密码");
                return;
            }
            if (passwordData.password !== passwordData.confirmPassword) {
                setIsPasswordSuccess(false);
                setPasswordMessage("两次输入的密码不一致");
                return;
            }
            if (passwordData.password.length < 6) {
                setIsPasswordSuccess(false);
                setPasswordMessage("密码长度不能少于6个字符");
                return;
            }

            // 更新密码
            await new Promise<void>((resolve, reject) => {
                new ModifyPasswordMessage(userToken, passwordData.oldPassword, passwordData.password).send(
                    (info: string) => {
                        resolve();
                    },
                    (error: string) => {
                        reject(new Error(error));
                    }
                );
            });

            setIsPasswordSuccess(true);
            setPasswordMessage("密码修改成功！");

            // 清空密码字段
            setPasswordData({
                oldPassword: "",
                password: "",
                confirmPassword: ""
            });

        } catch (error) {
            setIsPasswordSuccess(false);
            setPasswordMessage(error instanceof Error ? error.message : '密码修改失败');
        } finally {
            setSavingPassword(false);
        }
    };

    return (
        <div className="home-settings-section">
            <h3 className="home-section-title">修改密码</h3>
            <form className="home-settings-form" onSubmit={handlePasswordSubmit}>
                <div className="home-form-group">
                    <label htmlFor="oldPassword">旧密码</label>
                    <input
                        type="password"
                        id="oldPassword"
                        name="oldPassword"
                        value={passwordData.oldPassword}
                        onChange={handlePasswordChange}
                        placeholder="输入旧密码"
                    />
                </div>

                <div className="home-form-group">
                    <label htmlFor="password">新密码</label>
                    <input
                        type="password"
                        id="password"
                        name="password"
                        value={passwordData.password}
                        onChange={handlePasswordChange}
                        placeholder="输入新密码"
                    />
                </div>

                <div className="home-form-group">
                    <label htmlFor="confirmPassword">确认密码</label>
                    <input
                        type="password"
                        id="confirmPassword"
                        name="confirmPassword"
                        value={passwordData.confirmPassword}
                        onChange={handlePasswordChange}
                        placeholder="再次输入新密码"
                    />
                </div>

                {passwordMessage && (
                    isPasswordSuccess ? (
                        <div className="home-settings-success-message">
                            <div className="home-settings-success-icon">✓</div>
                            <div className="home-settings-message-text">{passwordMessage}</div>
                        </div>
                    ) : (
                        <div className="home-settings-error-message">
                            <div className="home-settings-error-icon">!</div>
                            <div className="home-settings-message-text">{passwordMessage}</div>
                        </div>
                    )
                )}

                <button
                    type="submit"
                    className="home-save-btn"
                    disabled={savingPassword}
                >
                    {savingPassword ? "修改中..." : "修改密码"}
                </button>
            </form>
        </div>
    );
};

export default PasswordForm;
