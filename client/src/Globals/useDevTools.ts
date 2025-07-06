// src/Globals/useDevTools.ts
// React Hook 用于在组件中注册开发者工具

import { useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import devTools from './DevTools';

export const useDevTools = () => {
    const navigate = useNavigate();
    
    useEffect(() => {
        // 只在开发环境中注册 navigate 函数
        if (process.env.NODE_ENV === 'development') {
            devTools.setNavigate(navigate);
        }
        
        // 组件卸载时清理
        return () => {
            if (process.env.NODE_ENV === 'development') {
                devTools.navigate = undefined;
            }
        };
    }, [navigate]);
};

export default useDevTools;
