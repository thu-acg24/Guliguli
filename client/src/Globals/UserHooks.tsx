import { useEffect, useRef } from 'react'
import { useUserToken, useUserIDValue, useUserID } from './GlobalStore'
import { fetchUserID, refreshUserInfo, refreshUserStat } from './UserService'
import { setUserID, setUserInfo, setUserStat } from './GlobalStore'

// 用于管理全局用户状态副作用的 Hook
export const useGlobalUserEffects = () => {
    const userToken = useUserToken()
    const userID = useUserIDValue()

    // 当 userToken 变化时，获取 userID 或清空用户数据
    useEffect(() => {
        fetchUserID(userToken)
    }, [userToken])

    // 当 userID 变化时，获取用户信息和统计信息
    useEffect(() => {
        console.log('UserID changed: ->', userID)
        refreshUserInfo(userID)
        refreshUserStat(userID)
    }, [userID])
}

// 用于手动刷新用户信息的 Hook
export const useRefreshUserInfo = () => {
    const { userID } = useUserID()

    return async () => {
        await refreshUserInfo(userID)
    }
}

// 用于手动刷新用户统计信息的 Hook
export const useRefreshUserStat = () => {
    const { userID } = useUserID()

    return async () => {
        await refreshUserStat(userID)
    }
}
