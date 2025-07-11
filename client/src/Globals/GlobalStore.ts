import { create } from 'zustand'
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo'
import { UserStat } from 'Plugins/UserService/Objects/UserStat'

interface GlobalState {
    userToken: string | null
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
    setUserToken: (token: string | null) => void
    setUserID: (id: number | null) => void
    setUserInfo: (info: UserInfo | null) => void
    setUserStat: (stat: UserStat | null) => void
    setLoading: (key: keyof GlobalState['loading'], loading: boolean) => void
    setError: (key: keyof GlobalState['error'], error: string | null) => void
    clearUserData: () => void
}

const globalStore = create<GlobalState & GlobalActions>((set) => ({
    userToken: null,
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

    setUserToken: (token: string | null) => set({ userToken: token }),
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
export const setUserToken = (userToken: string | null) => globalStore.getState().setUserToken(userToken)
export const getUserID = () => globalStore.getState().userID
export const setUserID = (userID: number | null) => globalStore.getState().setUserID(userID)
export const getUserInfo = () => globalStore.getState().userInfo
export const setUserInfo = (userInfo: UserInfo | null) => globalStore.getState().setUserInfo(userInfo)
export const getUserStat = () => globalStore.getState().userStat
export const setUserStat = (userStat: UserStat | null) => globalStore.getState().setUserStat(userStat)
export const setLoading = (key: keyof GlobalState['loading'], loading: boolean) => globalStore.getState().setLoading(key, loading)
export const setError = (key: keyof GlobalState['error'], error: string | null) => globalStore.getState().setError(key, error)
export const clearUserData = () => globalStore.getState().clearUserData()

// 开发环境下的调试功能已经集成到 DevTools 中
// 请使用 devTools.store 来访问全局状态调试功能
if (typeof window !== 'undefined' && process.env.NODE_ENV !== 'production') {
    console.log('🔧 Global Store debugging available via devTools.store');
}
