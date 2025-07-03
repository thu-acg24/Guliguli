import React, { useState } from "react";
import { RegisterMessage } from "Plugins/UserService/APIs/RegisterMessage";
import "./Modal.css";

interface RegisterModalProps {
    isOpen: boolean;
    onClose: () => void;
}

const RegisterModal: React.FC<RegisterModalProps> = ({ isOpen, onClose }) => {
    const [username, setUsername] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [message, setMessage] = useState("");
    const [isSuccess, setIsSuccess] = useState(false);
    const [isClosing, setIsClosing] = useState(false);

    if (!isOpen && !isClosing) return null;

    const handleClose = () => {
        setIsClosing(true);
        setTimeout(() => {
            setIsClosing(false);
            setUsername("");
            setEmail("");
            setPassword("");
            setConfirmPassword("");
            setMessage("");
            onClose();
        }, 100);
    };

    const handleBackdropClick = (e: React.MouseEvent) => {
        if (e.target === e.currentTarget) {
            handleClose();
        }
    };

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        // 更严格的用户名校验：非空，长度3-20
        const trimmedUsername = username.trim();
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
        // 更严格的邮箱校验
        const trimmedEmail = email.trim();
        if (!trimmedEmail) {
            setIsSuccess(false);
            setMessage("请输入邮箱");
            return;
        }
        const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
        if (!emailRegex.test(trimmedEmail)) {
            setIsSuccess(false);
            setMessage("请输入有效的邮箱地址");
            return;
        }
        if (!password) {
            setIsSuccess(false);
            setMessage("请输入密码");
            return;
        }
        if (password !== confirmPassword) {
            setIsSuccess(false);
            setMessage("两次输入的密码不一致");
            return;
        }

        try {
            new RegisterMessage(username, email, password).send(
                (info: string) => {
                    // 注册成功时显示成功消息
                    setIsSuccess(true);
                    setMessage("注册成功！");

                    // 3秒后自动关闭模态框
                    setTimeout(() => {
                        handleClose();
                    }, 1000);
                }, (e) => {
                    setIsSuccess(false);
                    setMessage(e || '注册失败！');
                }
            );
        } catch (e) {
            setIsSuccess(false);
            setMessage(e.message || '注册失败！');
        }
    }

    return (
        <div className="modal-modal" onClick={handleBackdropClick}>
            <div className={`modal-modal-content ${isClosing ? 'closing' : ''}`} onClick={(e) => e.stopPropagation()}>
                <div className="modal-modal-header">
                    <div className="modal-modal-title">注册</div>
                    <div className="modal-modal-close" onClick={handleClose}>&times;</div>
                </div>
                <div className="modal-modal-body">
                    <form className="modal-login-form" onSubmit={handleSubmit}>
                        <div className="modal-form-group">
                            <label className="modal-form-label">用户名</label>
                            <input
                                type="text"
                                className="modal-form-input"
                                placeholder="请输入用户名"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                required
                            />
                        </div>
                        <div className="modal-form-group">
                            <label className="modal-form-label">邮箱</label>
                            <input
                                type="email"
                                className="modal-form-input"
                                placeholder="请输入邮箱"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                            />
                        </div>
                        <div className="modal-form-group">
                            <label className="modal-form-label">密码</label>
                            <input
                                type="password"
                                className="modal-form-input"
                                placeholder="请输入密码"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                        </div>
                        <div className="modal-form-group">
                            <label className="modal-form-label">确认密码</label>
                            <input
                                type="password"
                                className="modal-form-input"
                                placeholder="请再次输入密码"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                required
                            />
                        </div>
                        {message && (
                            isSuccess ? (
                                <div className="modal-success-message">
                                    <div className="modal-success-icon">✓</div>
                                    <div className="modal-modal-message-text">{message}</div>
                                </div>
                            ) : (
                                <div className="modal-error-message">
                                    <div className="modal-error-icon">!</div>
                                    <div className="modal-modal-message-text">{message}</div>
                                </div>
                            )
                        )}
                        <button type="submit" className="modal-login-btn">
                            注册
                        </button>
                        <div className="modal-register-link">
                            已有账户？<a href="#" onClick={(e) => {
                                e.preventDefault();
                                handleClose();
                            }}>返回登录</a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default RegisterModal;