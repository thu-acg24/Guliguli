// src/Globals/DevTools.ts
// 开发者工具 - 提供在浏览器控制台中调试的功能

import { NavigateFunction } from 'react-router-dom';
import { getUserInfo, getUserID, getUserToken, getUserStat, setUserInfo, setUserID, setUserToken, clearUserData } from './GlobalStore';
import * as Navigate from 'Globals/Navigate';

// 全局开发者工具对象
interface DevTools {
    navigate?: NavigateFunction;
    setNavigate: (navigate: NavigateFunction) => void;
    
    // 导航相关
    goToAudit: () => void;
    goToMember: () => void;
    goToMessage: () => void;
    goToHome: (userId: string) => void;
    goToVideo: (videoId: string) => void;
    goToMainPage: () => void;
    goBack: () => void;
    goForward: () => void;
    
    // 全局状态相关
    store: {
        getUserInfo: () => any;
        getUserID: () => number | null;
        getUserToken: () => string;
        getUserStat: () => any;
        setUserInfo: (info: any) => void;
        setUserID: (id: number | null) => void;
        setUserToken: (token: string) => void;
        clearUserData: () => void;
        getCurrentUser: () => { id: number | null; token: string; info: any };
        loginAs: (userId: number, token: string) => void;
    };
}

const devTools: DevTools = {
    navigate: undefined,
    
    // 设置 navigate 函数
    setNavigate(navigate: NavigateFunction) {
        this.navigate = navigate;
        console.log('🚀 Navigate function is now available in devTools');
    },
    
    // 快捷导航方法
    goToAudit() {
        if (this.navigate) {
            const { navigateAudit } = Navigate.useNavigateAudit();
            navigateAudit();
            console.log('📍 Navigated to audit page');
        } else {
            console.warn('❌ Navigate function not available');
        }
    },

    goToMember() {
        if (this.navigate) {
            const { navigateMember } = Navigate.useNavigateMember();
            navigateMember();
            console.log('📍 Navigated to member page');
        } else {
            console.warn('❌ Navigate function not available');
        }
    },

    goToMessage() {
        if (this.navigate) {
            const { navigateMessage } = Navigate.useNavigateMessage();
            navigateMessage();
            console.log('📍 Navigated to message page');
        } else {
            console.warn('❌ Navigate function not available');
        }
    },
    
    goToHome(userId: string) {
        if (this.navigate) {
            const { navigateHome } = Navigate.useNavigateHome();
            navigateHome(userId);
            console.log(`📍 Navigated to home page for user: ${userId}`);
        } else {
            console.warn('❌ Navigate function not available');
        }
    },
    
    goToVideo(videoId: string) {
        if (this.navigate) {
            const { navigateVideo } = Navigate.useNavigateVideo();
            navigateVideo(videoId);
            console.log(`📍 Navigated to video: ${videoId}`);
        } else {
            console.warn('❌ Navigate function not available');
        }
    },
    
    goToMainPage() {
        if (this.navigate) {
            const { navigateMain } = Navigate.useNavigateMain();
            navigateMain();
            console.log('📍 Navigated to main page');
        } else {
            console.warn('❌ Navigate function not available');
        }
    },
    
    goBack() {
        if (this.navigate) {
            this.navigate(-1);
            console.log('◀️ Navigated back');
        } else {
            console.warn('❌ Navigate function not available');
        }
    },
    
    goForward() {
        if (this.navigate) {
            this.navigate(1);
            console.log('▶️ Navigated forward');
        } else {
            console.warn('❌ Navigate function not available');
        }
    },
    
    // 全局状态管理
    store: {
        getUserInfo: () => {
            const info = getUserInfo();
            console.log('👤 Current user info:', info);
            return info;
        },
        
        getUserID: () => {
            const id = getUserID();
            console.log('🆔 Current user ID:', id);
            return id;
        },
        
        getUserToken: () => {
            const token = getUserToken();
            console.log('🔑 Current user token:', token ? `${token.substring(0, 10)}...` : 'null');
            return token;
        },
        
        getUserStat: () => {
            const stat = getUserStat();
            console.log('📊 Current user stat:', stat);
            return stat;
        },
        
        setUserInfo: (info: any) => {
            setUserInfo(info);
            console.log('✅ User info updated:', info);
        },
        
        setUserID: (id: number | null) => {
            setUserID(id);
            console.log('✅ User ID updated:', id);
        },
        
        setUserToken: (token: string) => {
            setUserToken(token);
            console.log('✅ User token updated:', token ? `${token.substring(0, 10)}...` : 'null');
        },
        
        clearUserData: () => {
            clearUserData();
            console.log('🧹 User data cleared');
        },
        
        getCurrentUser: () => {
            const user = {
                id: getUserID(),
                token: getUserToken(),
                info: getUserInfo()
            };
            console.log('👤 Current user:', user);
            return user;
        },
        
        loginAs: (userId: number, token: string) => {
            setUserID(userId);
            setUserToken(token);
            console.log(`✅ Logged in as user ${userId} with token ${token.substring(0, 10)}...`);
        }
    }
};

// 仅在开发环境中添加到全局对象
if (process.env.NODE_ENV === 'development') {
    (window as any).devTools = devTools;
    (window as any).navigate = (path: string | number, options?: any) => {
        if (devTools.navigate) {
            if (typeof path === 'number') {
                devTools.navigate(path);
            } else {
                devTools.navigate(path, options);
            }
            console.log(`📍 Navigated to: ${path}`);
        } else {
            console.warn('❌ Navigate function not available');
        }
    };
    
    console.log('🛠️ DevTools loaded! Available commands:');
    console.log('');
    console.log('📍 Navigation:');
    console.log('  - navigate("/path") - Navigate to any path');
    console.log('  - devTools.goToHome("123") - Go to user home page');
    console.log('  - devTools.goToVideo("456") - Go to video page');
    console.log('  - devTools.goToMainPage() - Go to main page');
    console.log('  - devTools.goBack() - Go back');
    console.log('  - devTools.goForward() - Go forward');
    console.log('');
    console.log('👤 User State Management:');
    console.log('  - devTools.store.getCurrentUser() - Get current user info');
    console.log('  - devTools.store.getUserInfo() - Get user profile');
    console.log('  - devTools.store.getUserID() - Get user ID');
    console.log('  - devTools.store.getUserToken() - Get user token');
    console.log('  - devTools.store.loginAs(123, "token") - Login as specific user');
    console.log('  - devTools.store.clearUserData() - Clear user data');
    console.log('');
    console.log('💡 Quick Actions:');
    console.log('  - devTools.store.getCurrentUser() then devTools.goToHome(result.id) - Go to my home');
    console.log('  - devTools.store.loginAs(123, "token") then devTools.goToHome("123") - Login and go to home');
}

export default devTools;
