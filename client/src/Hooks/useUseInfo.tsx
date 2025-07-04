import { useState, useEffect } from 'react';
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { QueryUserInfoMessage } from 'Plugins/UserService/APIs/QueryUserInfoMessage';
import { useUserID } from './useUserID';
export const useUserInfo = () => {
	const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
	const { userID } = useUserID();

	const fetchOtherUserInfo = async (userID: number): Promise<UserInfo> => {
		return new Promise<UserInfo>((resolve, reject) => {
			try {
				new QueryUserInfoMessage(userID).send(
					(info: string) => {
						const userInfo = JSON.parse(info);
						resolve(userInfo);
					},
					(e: string) => {
						console.error("获取用户信息失败:", e);
						reject(new Error('未找到用户'));
					}
				);
			} catch (e) {
				console.error("获取用户信息异常:", e.message);
				throw new Error(`获取用户信息失败: ${e}`);
			}
		});
	};

	const refreshUserInfo = async () => {
		if (userID) {
			try {
				new QueryUserInfoMessage(userID).send(
					(info: string) => {
						const userInfo = JSON.parse(info);
						setUserInfo(userInfo);
					},
					(e: string) => {
						console.error("获取用户信息失败:", e);
					}
				);
			} catch (e) {
				console.error("获取用户信息异常:", e.message);
			}
		} else {
			setUserInfo(null);
		}
	}

	useEffect(() => {
		refreshUserInfo();
	}, [userID]);

	return { userInfo, refreshUserInfo, fetchOtherUserInfo };
};