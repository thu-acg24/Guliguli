import React, { useState, useEffect, useCallback } from 'react';
import './TopSuccessToast.css';

export const useTopSuccessToast = () => {
    const [toastState, setToastState] = useState({
        visible: false,
        message: ''
    });

    const showSuccess = useCallback((message: string) => {
        setToastState({
            visible: true,
            message
        });
    }, []);

    const hideToast = useCallback(() => {
        setToastState(prev => ({
            ...prev,
            visible: false
        }));
    }, []);

    useEffect(() => {
        if (toastState.visible) {
            const timer = setTimeout(() => {
                hideToast();
            }, 2000);

            return () => clearTimeout(timer);
        }
    }, [toastState.visible, hideToast]);

    const ToastComponent = toastState.visible ? (
        <div className="top-success-toast">
            <div className="toast-content">
                <span className="toast-icon">✅</span>
                <span className="toast-message">{toastState.message}</span>
            </div>
        </div>
    ) : null;

    return {
        ToastComponent,
        showSuccess
    };
};
