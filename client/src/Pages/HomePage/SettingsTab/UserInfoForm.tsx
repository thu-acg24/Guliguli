// src/Pages/HomePage/SettingsTab/UserInfoForm.tsx
import React, { useState, useRef } from "react";
import { useUserToken } from "Globals/GlobalStore";
import { useRefreshUserInfo } from "Globals/UserHooks";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { ModifyAvatarMessage } from "Plugins/UserService/APIs/ModifyAvatarMessage";
import { ValidateAvatarMessage } from "Plugins/UserService/APIs/ValidateAvatarMessage";
import { ModifyUserInfoMessage } from "Plugins/UserService/APIs/ModifyUserInfoMessage";
import { validateUsername } from "Components/LoginModal/ValidateUserInfo";
import "../HomePage.css";

const UserInfoForm: React.FC<{
    userInfo?: any;
    refreshHomePageUserInfo?: () => void;
}> = ({
    userInfo,
    refreshHomePageUserInfo
}) => {
    const fileInputRef = useRef<HTMLInputElement>(null);

    const userToken = useUserToken();
    const refreshUserInfo = useRefreshUserInfo();
    const [formData, setFormData] = useState({
        username: userInfo?.username || '',
        avatar: userInfo?.avatarPath || ''
    });
    const [saving, setSaving] = useState(false);
    const [uploading, setUploading] = useState(false);
    const [message, setMessage] = useState("");
    const [isSuccess, setIsSuccess] = useState(false);
    const [avatarSessionToken, setAvatarSessionToken] = useState<string | null>(null);

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
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
            const trimmedUsername = validateUsername(formData.username);

            // 如果有新上传的头像，先验证头像
            if (avatarSessionToken) {
                await new Promise<void>((resolve, reject) => {
                    new ValidateAvatarMessage(avatarSessionToken).send(
                        (info: string) => { resolve();},
                        (error: string) => { reject(new Error(error));}
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
                    userInfo?.isBanned || false,
                    userInfo?.bio || ''
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

    return (
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
                            multiple={false}
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
    );
};

export default UserInfoForm;
