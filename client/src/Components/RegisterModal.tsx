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
    const [isClosing, setIsClosing] = useState(false);

    if (!isOpen && !isClosing) return null;

    const handleClose = () => {
        setIsClosing(true);
        setTimeout(() => {
            setIsClosing(false);
            onClose();
        }, 100); // 与动画时长保持一致
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
            setMessage("请输入用户名");
            return;
        }
        if (!email.trim()) {
            setMessage("请输入邮箱");
            return;
        }
        if (!password) {
            setMessage("请输入密码");
            return;
        }
        if (password !== confirmPassword) {
            setMessage("两次输入的密码不一致");
            return;
        }

        // 简单的邮箱格式验证
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            setMessage("请输入有效的邮箱地址");
            return;
        }

        try {
            new RegisterMessage(username, email, password).send(
                (info: string) => {
                    handleClose();
                }, (e) => {
                    setMessage(e || '注册失败！');
                }
            );
        } catch (e) {
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
                            <div className="error-message">
                                <div className="error-icon">!</div>
                                <div className="error-text">{message}</div>
                            </div>
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
