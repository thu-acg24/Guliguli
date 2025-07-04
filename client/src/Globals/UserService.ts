import { UserInfo } from 'Plugins/UserService/Objects/UserInfo'
import { UserStat } from 'Plugins/UserService/Objects/UserStat'
import { GetUIDByTokenMessage } from 'Plugins/UserService/APIs/GetUIDByTokenMessage'
import { QueryUserInfoMessage } from 'Plugins/UserService/APIs/QueryUserInfoMessage'
import { QueryUserStatMessage } from 'Plugins/UserService/APIs/QueryUserStatMessage'
import { materialAlertError } from 'Plugins/CommonUtils/Gadgets/AlertGadget'
import { 
    setUserID, 
    setUserInfo, 
    setUserStat, 
    setLoading, 
    setError, 
    clearUserData,
    setUserToken
} from './GlobalStore'

// 根据 token 获取用户 ID
export const fetchUserID = async (userToken: string): Promise<void> => {
    if (!userToken) {
        setUserID(null)
        setError('userID', null)
        return
    }

    setLoading('userID', true)
    setError('userID', null)

    try {
        const id = await new Promise<number>((resolve, reject) => {
            try {
                new GetUIDByTokenMessage(userToken).send(
                    (info: string) => {
                        const userID = JSON.parse(info)
                        resolve(userID)
                    },
                    (e) => {
                        console.error('Token校验失败:', e)
                        materialAlertError(`Token校验失败`, "", () => {
                            setUserToken("")
                        })
                        reject(new Error('Token校验失败'))
                    }
                )
            } catch (e) {
                console.error('Token校验异常:', e)
                materialAlertError(`Token校验失败`, "", () => {
                    setUserToken("")
                })
                reject(new Error('Token校验失败'))
            }
        })
        setUserID(id)
    } catch (err) {
        setError('userID', err instanceof Error ? err.message : '获取用户ID失败')
        setUserID(null)
    } finally {
        setLoading('userID', false)
    }
}

// 刷新用户信息
export const refreshUserInfo = async (userID: number): Promise<void> => {
    console.log("!!!!!!!!!!!!!!!!! 1")
    if (!userID) {
        setUserInfo(null)
        setError('userInfo', null)
        return
    }

    setLoading('userInfo', true)
    setError('userInfo', null)
    console.log("!!!!!!!!!!!!!!!!! 2")

    try {
        await new Promise<void>((resolve, reject) => {
            new QueryUserInfoMessage(userID).send(
                (info: string) => {
                    const userInfo = JSON.parse(info)
                    console.log("!!!!!!!!!!!!!!!!!获取用户信息成功:", userInfo)
                    setUserInfo(userInfo)
                    resolve()
                },
                (e: string) => {
                    console.error("获取用户信息失败:", e)
                    reject(new Error(e))
                }
            )
        })
    } catch (err) {
        setError('userInfo', err instanceof Error ? err.message : '获取用户信息失败')
    } finally {
        setLoading('userInfo', false)
    }
}

// 刷新用户统计信息
export const refreshUserStat = async (userID: number): Promise<void> => {
    if (!userID) {
        setUserStat(null)
        setError('userStat', null)
        return
    }

    setLoading('userStat', true)
    setError('userStat', null)

    try {
        await new Promise<void>((resolve, reject) => {
            new QueryUserStatMessage(userID).send(
                (info: string) => {
                    const userStat = JSON.parse(info)
                    setUserStat(userStat)
                    resolve()
                },
                (e: string) => {
                    console.error("获取用户统计信息失败:", e)
                    reject(new Error(e))
                }
            )
        })
    } catch (err) {
        setError('userStat', err instanceof Error ? err.message : '获取用户统计信息失败')
    } finally {
        setLoading('userStat', false)
    }
}

// 获取其他用户信息
export const fetchOtherUserInfo = async (userID: number): Promise<UserInfo> => {
    return new Promise<UserInfo>((resolve, reject) => {
        try {
            new QueryUserInfoMessage(userID).send(
                (info: string) => {
                    const userInfo = JSON.parse(info)
                    resolve(userInfo)
                },
                (e: string) => {
                    console.error("获取用户信息失败:", e)
                    reject(new Error('未找到用户'))
                }
            )
        } catch (e) {
            console.error("获取用户信息异常:", e instanceof Error ? e.message : e)
            reject(new Error(`获取用户信息失败: ${e}`))
        }
    })
}

// 获取其他用户统计信息
export const fetchOtherUserStat = async (userID: number): Promise<UserStat> => {
    return new Promise<UserStat>((resolve, reject) => {
        try {
            new QueryUserStatMessage(userID).send(
                (info: string) => {
                    const userStat = JSON.parse(info)
                    resolve(userStat)
                },
                (e: string) => {
                    console.error("获取用户统计信息失败:", e)
                    reject(new Error('未找到用户统计信息'))
                }
            )
        } catch (e) {
            console.error("获取用户统计信息异常:", e instanceof Error ? e.message : e)
            reject(new Error(`获取用户统计信息失败: ${e}`))
        }
    })
}
