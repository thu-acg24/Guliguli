import React, { useState } from "react";
import { RegisterMessage } from "Plugins/UserService/APIs/RegisterMessage";

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

        // 验证输入
        if (!username.trim()) {
            setIsSuccess(false);
            setMessage("请输入用户名");
            return;
        }
        if (!email.trim()) {
            setIsSuccess(false);
            setMessage("请输入邮箱");
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

        // 简单的邮箱格式验证
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            setIsSuccess(false);
            setMessage("请输入有效的邮箱地址");
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
        <div className="modal" onClick={handleBackdropClick}>
            <div className={`modal-content ${isClosing ? 'closing' : ''}`} onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <div className="modal-title">注册</div>
                    <div className="modal-close" onClick={handleClose}>&times;</div>
                </div>
                <div className="modal-body">
                    <form className="login-form" onSubmit={handleSubmit}>
                        <div className="form-group">
                            <label className="form-label">用户名</label>
                            <input
                                type="text"
                                className="form-input"
                                placeholder="请输入用户名"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">邮箱</label>
                            <input
                                type="email"
                                className="form-input"
                                placeholder="请输入邮箱"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">密码</label>
                            <input
                                type="password"
                                className="form-input"
                                placeholder="请输入密码"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                        </div>
                        <div className="form-group">
                            <label className="form-label">确认密码</label>
                            <input
                                type="password"
                                className="form-input"
                                placeholder="请再次输入密码"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                required
                            />
                        </div>
                        {message && (
                            isSuccess ? (
                                <div className="success-message">
                                    <div className="success-icon">✓</div>
                                    <div className="modal-message-text">{message}</div>
                                </div>
                            ) : (
                                <div className="error-message">
                                    <div className="error-icon">!</div>
                                    <div className="modal-message-text">{message}</div>
                                </div>
                            )
                        )}
                        <button type="submit" className="login-btn">
                            注册
                        </button>
                        <div className="register-link">
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