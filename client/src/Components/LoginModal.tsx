import React, { useState } from "react";
import { setUserToken } from "Globals/GlobalStore"
import { LoginMessage } from "Plugins/UserService/APIs/LoginMessage";
import RegisterModal from "./RegisterModal";

interface LoginModalProps {
    isOpen: boolean;
    onClose: () => void;
}

const LoginModal: React.FC<LoginModalProps> = ({ isOpen, onClose }) => {
    const [showRegisterModal, setShowRegisterModal] = useState(false);
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
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
        try {
            new LoginMessage(username, password).send(
                (info: string) => {
                    const token = JSON.parse(info);
                    setUserToken(token);
                    handleClose();
                }, (e) => {
                    setMessage(e || '登录失败！');
                }
            );
        } catch (e) {
            setMessage(e.message || '登录失败！');
        }
    }

    return (
        <>
            <div className="modal" onClick={handleBackdropClick}>
                <div className={`modal-content ${isClosing ? 'closing' : ''}`} onClick={(e) => e.stopPropagation()}>
                    <div className="modal-header">
                        <div className="modal-title">登录</div>
                        <div className="modal-close" onClick={handleClose}>&times;</div>
                    </div>
                    <div className="modal-body">
                        <form className="login-form" onSubmit={handleSubmit}>
                            <div className="form-group">
                                <label className="form-label">用户名</label>
                                <input type="text" className="form-input" placeholder="请输入用户名"
                                    value={username} onChange={(e) => setUsername(e.target.value)} />
                            </div>
                            <div className="form-group">
                                <label className="form-label">密码</label>
                                <input type="password" className="form-input" placeholder="请输入密码"
                                    value={password} onChange={(e) => setPassword(e.target.value)} />
                            </div>
                            {message && (
                                <div className="error-message">
                                    <div className="error-icon">!</div>
                                    <div className="error-text">{message}</div>
                                </div>
                            )}
                            <button type="submit" className="login-btn">
                                登录
                            </button>
                            <div className="register-link">
                                还没有账户？<a href="#" onClick={(e) => {
                                    e.preventDefault();
                                    setShowRegisterModal(true);
                                }}>点击注册</a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
            <RegisterModal
                isOpen={showRegisterModal}
                onClose={() => setShowRegisterModal(false)}
            />
        </>
    );
};

export default LoginModal;
