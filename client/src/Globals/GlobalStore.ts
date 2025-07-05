import { create } from 'zustand'
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo'
import { UserStat } from 'Plugins/UserService/Objects/UserStat'

interface GlobalState {
    userToken: string
    userID: number | null
    userInfo: UserInfo | null
    userStat: UserStat | null
    loading: {
        userID: boolean
        userInfo: boolean
        userStat: boolean
    }
    error: {
        userID: string | null
        userInfo: string | null
        userStat: string | null
    }
}

interface GlobalActions {
    setUserToken: (token: string) => void
    setUserID: (id: number | null) => void
    setUserInfo: (info: UserInfo | null) => void
    setUserStat: (stat: UserStat | null) => void
    setLoading: (key: keyof GlobalState['loading'], loading: boolean) => void
    setError: (key: keyof GlobalState['error'], error: string | null) => void
    clearUserData: () => void
}

const globalStore = create<GlobalState & GlobalActions>((set) => ({
    userToken: "",
    userID: null,
    userInfo: null,
    userStat: null,
    loading: {
        userID: false,
        userInfo: false,
        userStat: false
    },
    error: {
        userID: null,
        userInfo: null,
        userStat: null
    },

    setUserToken: (token: string) => set({ userToken: token }),
    setUserID: (id: number | null) => set({ userID: id }),
    setUserInfo: (info: UserInfo | null) => set({ userInfo: info }),
    setUserStat: (stat: UserStat | null) => set({ userStat: stat }),

    setLoading: (key: keyof GlobalState['loading'], loading: boolean) => 
        set(state => ({ loading: { ...state.loading, [key]: loading } })),

    setError: (key: keyof GlobalState['error'], error: string | null) => 
        set(state => ({ error: { ...state.error, [key]: error } })),

    clearUserData: () => set({
        userID: null,
        userInfo: null,
        userStat: null,
        loading: {
            userID: false,
            userInfo: false,
            userStat: false
        },
        error: {
            userID: null,
            userInfo: null,
            userStat: null
        }
    })
}))

// 导出 hooks - 使用最小选择器避免重新渲染
export const useUserToken = () => globalStore(state => state.userToken)
export const useUserIDValue = () => globalStore(state => state.userID)
export const useUserInfoValue = () => globalStore(state => state.userInfo)
export const useUserStatValue = () => globalStore(state => state.userStat)

// 复合选择器
export const useUserID = () => {
    const userID = useUserIDValue()
    const loading = globalStore(state => state.loading.userID)
    const error = globalStore(state => state.error.userID)
    return { userID, loading, error }
}

export const useUserInfo = () => {
    const userInfo = useUserInfoValue()
    const loading = globalStore(state => state.loading.userInfo)
    const error = globalStore(state => state.error.userInfo)
    return { userInfo, loading, error }
}

export const useUserStat = () => {
    const userStat = useUserStatValue()
    const loading = globalStore(state => state.loading.userStat)
    const error = globalStore(state => state.error.userStat)
    return { userStat, loading, error }
}

// 导出 actions
export const getUserToken = () => globalStore.getState().userToken
export const setUserToken = (userToken: string) => globalStore.getState().setUserToken(userToken)
export const getUserID = () => globalStore.getState().userID
export const setUserID = (userID: number | null) => globalStore.getState().setUserID(userID)
export const getUserInfo = () => globalStore.getState().userInfo
export const setUserInfo = (userInfo: UserInfo | null) => globalStore.getState().setUserInfo(userInfo)
export const getUserStat = () => globalStore.getState().userStat
export const setUserStat = (userStat: UserStat | null) => globalStore.getState().setUserStat(userStat)
export const setLoading = (key: keyof GlobalState['loading'], loading: boolean) => globalStore.getState().setLoading(key, loading)
export const setError = (key: keyof GlobalState['error'], error: string | null) => globalStore.getState().setError(key, error)
export const clearUserData = () => globalStore.getState().clearUserData()

// 开发环境下暴露到全局作用域用于调试
if (typeof window !== 'undefined' && process.env.NODE_ENV !== 'production') {
    (window as any).debugGlobalStore = {
        // 获取状态
        getUserInfo: () => globalStore.getState().userInfo,
        getUserID: () => globalStore.getState().userID,
        getUserToken: () => globalStore.getState().userToken,
        getUserStat: () => globalStore.getState().userStat,
        getLoading: () => globalStore.getState().loading,
        getError: () => globalStore.getState().error,
        
        // 获取完整状态
        getState: () => globalStore.getState(),
        
        // 设置状态（用于测试）
        setUserInfo: (info: any) => globalStore.getState().setUserInfo(info),
        setUserID: (id: number | null) => globalStore.getState().setUserID(id),
        setUserToken: (token: string) => globalStore.getState().setUserToken(token),
        
        // 清理数据
        clearUserData: () => globalStore.getState().clearUserData(),
        
        // 订阅状态变化
        subscribe: (callback: (state: any) => void) => globalStore.subscribe(callback)
    }
    
    console.log('🔧 Global Store Debug Interface Available: window.debugGlobalStore')
}
