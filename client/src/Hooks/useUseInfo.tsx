import React, { useState, useEffect, useRef } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { materialAlertError } from 'Plugins/CommonUtils/Gadgets/AlertGadget';
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';
import { QueryUserInfoMessage } from 'Plugins/UserService/APIs/QueryUserInfoMessage';
import { GetUIDByTokenMessage } from 'Plugins/UserService/APIs/GetUIDByTokenMessage';
export const useUserInfo = () => {
  const [userInfo, setUserInfo] = useState<UserInfo | null>(null);
  const userToken = useUserToken();

  const fetchOtherUserInfo = async (userID: number):Promise<UserInfo> => {
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
  
  const fetchUserInfo = async (userID: number) => {
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
  };

  const getUserIDByToken = async (userToken: string): Promise<number> => {
    return new Promise<number>((resolve, reject) => {
      try {
        new GetUIDByTokenMessage(userToken).send(
          (info: string) => {
            const userID = JSON.parse(info);
            resolve(userID);
          },
          (e) => {
            console.error('未找到用户', e);
            reject(new Error('未找到用户'));
          }
        );
      } catch (e) {
        console.error('未找到用户', e);
        reject(new Error('未找到用户'));
      }
    });
  };
  
  useEffect(() => {
    if (userToken) {
      getUserIDByToken(userToken)
        .then(userID => fetchUserInfo(userID))
        .catch(console.error);
    }
  }, [userToken]);

  return { userInfo, fetchUserInfo, getUserIDByToken, fetchOtherUserInfo};
};