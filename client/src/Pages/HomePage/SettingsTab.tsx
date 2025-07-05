// src/Pages/HomePage/SettingsTab.tsx
import React, { useState, useRef } from "react";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { useOutletContext } from "react-router-dom";
import { useUserToken } from "Globals/GlobalStore";
import { useRefreshUserInfo } from "Globals/UserHooks";
import { ModifyAvatarMessage } from "Plugins/UserService/APIs/ModifyAvatarMessage";
import { ValidateAvatarMessage } from "Plugins/UserService/APIs/ValidateAvatarMessage";
import { ModifyUserInfoMessage } from "Plugins/UserService/APIs/ModifyUserInfoMessage";
import { ModifyPasswordMessage } from "Plugins/UserService/APIs/ModifyPasswordMessage";
import "./HomePage.css";

const SettingsTab: React.FC<{ userInfo?: any }> = (props) => {
    const outlet = useOutletContext<{ userInfo: any, refreshUserInfo?: () => void }>();
    const userInfo = props.userInfo ?? outlet?.userInfo;
    const refreshHomePageUserInfo = outlet?.refreshUserInfo;
    const userToken = useUserToken();
    const refreshUserInfo = useRefreshUserInfo();

    const fileInputRef = useRef<HTMLInputElement>(null);

    const [formData, setFormData] = useState({
        username: userInfo?.username || '',
        avatar: userInfo?.avatarPath || ''
    });
    const [passwordData, setPasswordData] = useState({
        oldPassword: "",
        password: "",
        confirmPassword: ""
    });
    const [saving, setSaving] = useState(false);
    const [savingPassword, setSavingPassword] = useState(false);
    const [uploading, setUploading] = useState(false);
    const [message, setMessage] = useState("");
    const [isSuccess, setIsSuccess] = useState(false);
    const [passwordMessage, setPasswordMessage] = useState("");
    const [isPasswordSuccess, setIsPasswordSuccess] = useState(false);
    const [avatarSessionToken, setAvatarSessionToken] = useState<string | null>(null);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setPasswordData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleAvatarUpload = async () => {
        if (!userToken) {
            setIsSuccess(false);
            setMessage("请先登录");
            return;
        }

        const input = fileInputRef.current;
        if (!input) return;

        input.click();
    };

    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        // 验证文件类型
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];
        if (!allowedTypes.includes(file.type)) {
            setIsSuccess(false);
            setMessage("只支持 JPEG、PNG、GIF 和 WebP 格式的图片");
            return;
        }

        // 验证文件大小 (5MB)
        const maxSize = 5 * 1024 * 1024;
        if (file.size > maxSize) {
            setIsSuccess(false);
            setMessage("图片大小不能超过 5MB");
            return;
        }

        setUploading(true);
        setMessage("");

        try {
            // 第一步：获取上传 URL
            const uploadInfo = await new Promise<string>((resolve, reject) => {
                new ModifyAvatarMessage(userToken).send(
                    (info: string) => {
                        resolve(info);
                    },
                    (error: string) => {
                        reject(new Error(error));
                    }
                );
            });

            const uploadData = JSON.parse(uploadInfo);
            const [uploadUrl, sessionToken] = uploadData;

            // 第二步：上传文件
            const uploadResponse = await fetch(uploadUrl, {
                method: 'PUT',
                body: file,
                headers: {
                    'Content-Type': file.type,
                },
            });

            if (!uploadResponse.ok) {
                throw new Error('上传失败');
            }

            // 保存 sessionToken，等待用户保存个人信息时再验证
            setAvatarSessionToken(sessionToken);

            // 更新本地头像预览
            const avatarUrl = URL.createObjectURL(file);
            setFormData(prev => ({
                ...prev,
                avatar: avatarUrl
            }));
            setIsSuccess(true);
            setMessage("头像上传成功！请点击保存个人信息完成头像更换");
        } catch (error) {
            setIsSuccess(false);
            setMessage(error instanceof Error ? error.message : '头像上传失败');
        } finally {
            setUploading(false);
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();

        if (!userToken) {
            setIsSuccess(false);
            setMessage("请先登录");
            return;
        }

        setSaving(true);
        setMessage("");

        try {
            // 验证用户名
            const trimmedUsername = formData.username.trim();
            if (!trimmedUsername) {
                setIsSuccess(false);
                setMessage("请输入用户名");
                return;
            }
            if (trimmedUsername.length < 3 || trimmedUsername.length > 20) {
                setIsSuccess(false);
                setMessage("用户名长度需为3-20个字符");
                return;
            }

            // 如果有新上传的头像，先验证头像
            if (avatarSessionToken) {
                await new Promise<void>((resolve, reject) => {
                    new ValidateAvatarMessage(avatarSessionToken).send(
                        (info: string) => {
                            // 验证成功，无需解析返回值
                            resolve();
                        },
                        (error: string) => {
                            reject(new Error(error));
                        }
                    );
                });
                // 验证成功后等待3秒钟
                await new Promise(resolve => setTimeout(resolve, 3000));
                // 验证成功后清除 sessionToken
                setAvatarSessionToken(null);
            }

            // 更新用户信息
            if (trimmedUsername !== userInfo?.username) {
                const newUserInfo = new UserInfo(
                    userInfo?.userID || 0,
                    trimmedUsername,
                    userInfo?.avatarPath || '',
                    userInfo?.isBanned || false
                );

                await new Promise<void>((resolve, reject) => {
                    new ModifyUserInfoMessage(userToken, newUserInfo).send(
                        (info: string) => {
                            resolve();
                        },
                        (error: string) => {
                            reject(new Error(error));
                        }
                    );
                });
            }

            setIsSuccess(true);
            setMessage("个人信息保存成功！");

            // 刷新全局用户信息
            await refreshUserInfo();

            // 刷新 HomePage 中的用户信息
            if (refreshHomePageUserInfo) {
                await refreshHomePageUserInfo();
            }

        } catch (error) {
            setIsSuccess(false);
            setMessage(error instanceof Error ? error.message : '保存失败');
        } finally {
            setSaving(false);
        }
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
        <div className="home-settings-tab">
            {/* 个人信息修改部分 */}
            <div className="home-settings-section">
                <h3 className="home-section-title">个人信息</h3>
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
                            <input
                                type="file"
                                ref={fileInputRef}
                                style={{ display: 'none' }}
                                accept="image/*"
                                onChange={handleFileChange}
                            />
                            <button
                                type="button"
                                className="home-upload-btn"
                                onClick={handleAvatarUpload}
                                disabled={uploading}
                            >
                                {uploading ? "上传中..." : "上传新头像"}
                            </button>
                        </div>
                    </div>

                    {message && (
                        isSuccess ? (
                            <div className="home-settings-success-message">
                                <div className="home-settings-success-icon">✓</div>
                                <div className="home-settings-message-text">{message}</div>
                            </div>
                        ) : (
                            <div className="home-settings-error-message">
                                <div className="home-settings-error-icon">!</div>
                                <div className="home-settings-message-text">{message}</div>
                            </div>
                        )
                    )}

                    <button
                        type="submit"
                        className="home-save-btn"
                        disabled={saving}
                    >
                        {saving ? "保存中..." : "保存个人信息"}
                    </button>
                </form>
            </div>

            {/* 密码修改部分 */}
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
        </div>
    );
};

export default SettingsTab;