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
        // 初次渲染时，lastTokenRef.current 为 undefined，会执行一次
        // 之后只有当 token 真正改变时才执行


        if (userToken) {
            // 有 token 时获取用户 ID
            fetchUserID(userToken)
        } else {
            // 无 token 时清空所有数据，并重置 userID ref
            setUserID(null)
        }
    }, [userToken])

    // 当 userID 变化时，获取用户信息和统计信息
    useEffect(() => {
        console.log('UserID changed: ->', userID)
        if (userID) {
            refreshUserInfo(userID)
            refreshUserStat(userID)
        } else if (!userID) {
            setUserInfo(null)
            setUserStat(null)
        }
    }, [userID])
}

// 用于手动刷新用户信息的 Hook
export const useRefreshUserInfo = () => {
    const { userID } = useUserID()

    return async () => {
        if (userID) {
            await refreshUserInfo(userID)
        }
    }
}

// 用于手动刷新用户统计信息的 Hook
export const useRefreshUserStat = () => {
    const { userID } = useUserID()

    return async () => {
        if (userID) {
            await refreshUserStat(userID)
        }
    }
}
