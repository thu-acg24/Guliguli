import { useState, useEffect } from 'react';
import { UserStat } from 'Plugins/UserService/Objects/UserStat';
import { QueryUserStatMessage } from 'Plugins/UserService/APIs/QueryUserStatMessage';
import { useUserID } from './useUserID';

export const useUserStat = () => {
    const [userStat, setUserStat] = useState<UserStat | null>(null);
    const { userID } = useUserID();

    const fetchOtherUserStat = async (userID: number): Promise<UserStat> => {
        return new Promise<UserStat>((resolve, reject) => {
            try {
                new QueryUserStatMessage(userID).send(
                    (info: string) => {
                        const userStat = JSON.parse(info);
                        resolve(userStat);
                    },
                    (e: string) => {
                        console.error("获取用户统计信息失败:", e);
                        reject(new Error('未找到用户统计信息'));
                    }
                );
            } catch (e) {
                console.error("获取用户统计信息异常:", e.message);
                throw new Error(`获取用户统计信息失败: ${e}`);
            }
        });
    };

    const refreshUserStat = async () => {
        if (userID) {
            try {
                new QueryUserStatMessage(userID).send(
                    (info: string) => {
                        const userStat = JSON.parse(info);
                        setUserStat(userStat);
                    },
                    (e: string) => {
                        console.error("获取用户统计信息失败:", e);
                    }
                );
            } catch (e) {
                console.error("获取用户统计信息异常:", e.message);
            }
        } else {
            setUserStat(null);
        }
    }

    useEffect(() => {
        refreshUserStat();
    }, [userID]);

    return { userStat, refreshUserStat, fetchOtherUserStat };
};
