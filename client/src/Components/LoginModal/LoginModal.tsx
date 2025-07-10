import React, { useState } from "react";
import { setUserToken } from "Globals/GlobalStore"
import { LoginMessage } from "Plugins/UserService/APIs/LoginMessage";
import RegisterModal from "./RegisterModal";
import "./Modal.css"

const LoginModal: React.FC<{
    isOpen: boolean;
    onClose: () => void;
}> = ({ isOpen, onClose }) => {
    const [showRegisterModal, setShowRegisterModal] = useState(false);
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");
    const [isSuccess, setIsSuccess] = useState(false);
    const [isClosing, setIsClosing] = useState(false);

    if (!isOpen && !isClosing) return null;

    const handleClose = () => {
        setIsClosing(true);
        setTimeout(() => {
            setIsClosing(false);
            setUsername("");
            setPassword("");
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
        try {
            new LoginMessage(username, password).send(
                (info: string) => {
                    const token = JSON.parse(info);
                    setUserToken(token);
                    setIsSuccess(true);
                    setMessage("登录成功！");

                    setTimeout(() => {
                        handleClose();
                    }, 1000);
                }, (e) => {
                    setIsSuccess(false);
                    setMessage(e || '登录失败！');
                }
            );
        } catch (e) {
            setIsSuccess(false);
            setMessage(e.message || '登录失败！');
        }
    }

    return (
        <>
            <div className="modal-modal" onClick={handleBackdropClick}>
                <div className={`modal-modal-content ${isClosing ? 'closing' : ''}`} onClick={(e) => e.stopPropagation()}>
                    <div className="modal-modal-header">
                        <div className="modal-modal-title">登录</div>
                        <div className="modal-modal-close" onClick={handleClose}>&times;</div>
                    </div>
                    <div className="modal-modal-body">
                        <form className="modal-login-form" onSubmit={handleSubmit}>
                            <div className="modal-form-group">
                                <label className="modal-form-label">用户名</label>
                                <input type="text" className="modal-form-input" placeholder="请输入用户名"
                                    value={username} onChange={(e) => setUsername(e.target.value)} />
                            </div>
                            <div className="modal-form-group">
                                <label className="modal-form-label">密码</label>
                                <input type="password" className="modal-form-input" placeholder="请输入密码"
                                    value={password} onChange={(e) => setPassword(e.target.value)} />
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
                                登录
                            </button>
                            <div className="modal-register-link">
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
