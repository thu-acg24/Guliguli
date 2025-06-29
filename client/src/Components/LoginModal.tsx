import React from "react";

interface LoginModalProps {
    isOpen: boolean;
    onClose: () => void;
}

const LoginModal: React.FC<LoginModalProps> = ({ isOpen, onClose }) => {
    if (!isOpen) return null;

    return (
        <div className="modal" onClick={onClose}>
            <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                <div className="modal-header">
                    <div className="modal-title">登录</div>
                    <div className="modal-close" onClick={onClose}>&times;</div>
                </div>
                <div className="modal-body">
                    <form className="login-form">
                        <div className="form-group">
                            <label className="form-label">用户名</label>
                            <input type="text" className="form-input" placeholder="请输入用户名" />
                        </div>
                        <div className="form-group">
                            <label className="form-label">密码</label>
                            <input type="password" className="form-input" placeholder="请输入密码" />
                        </div>
                        <button type="button" className="login-btn" onClick={() => {
                            // API call would go here for login
                            // Example: fetch('/api/login', { method: 'POST', body: JSON.stringify({ username, password }) })
                            alert('登录功能待实现');
                            onClose();
                        }}>
                            登录
                        </button>
                        <div className="register-link">
                            还没有账户？<a href="#" onClick={(e) => {
                                e.preventDefault();
                                alert('跳转到注册页面');
                                onClose();
                            }}>点击注册</a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default LoginModal;
