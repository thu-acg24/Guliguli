// src/Pages/HomePage/FollowersTab.tsx
import React, { useState, useEffect } from "react";
import { useOutletContext } from "react-router-dom";
import { useNavigateHome } from "Globals/Navigate";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { QueryFollowerListMessage } from "Plugins/UserService/APIs/QueryFollowerListMessage";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import { QueryFollowMessage } from "Plugins/UserService/APIs/QueryFollowMessage";
import { ChangeFollowStatusMessage } from "Plugins/UserService/APIs/ChangeFollowStatusMessage";
import { useUserIDValue, useUserToken } from "Globals/GlobalStore";
import "./HomePage.css";
import { setRef } from "@mui/material";

const perpage = 10; // 每次新显示的粉丝数量

// 定义包含关注状态的用户信息接口
interface FollowerWithStatus {
    userInfo: UserInfo;
    following: boolean;
}

const FollowersTab: React.FC = () => {
    const outlet = useOutletContext<{ userID: number, refreshUserStat: () => void, isFollowing: boolean }>();
    const userID = outlet.userID;
    const refreshUserStat = outlet.refreshUserStat;
    const isFollowing = outlet.isFollowing;

    const { navigateHome } = useNavigateHome();
    const currentUserID = useUserIDValue();
    const userToken = useUserToken();

    const [followers, setFollowers] = useState<FollowerWithStatus[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(true);
    const [refreshTag, setRefreshTag] = useState(false);

    useEffect(() => {
        // 重置状态
        setPage(1);
        setRefreshTag(prev => !prev);
    }, [isFollowing, userID]);

    // 获取用户的粉丝
    const fetchFollowers = async (page: number) => {
        setLoading(true);
        try {
            const newFollowersID = await new Promise<number[]>((resolve, reject) => {
                new QueryFollowerListMessage(userID, (page - 1) * perpage + 1, page * perpage + 1).send(
                    (info: string) => {
                        const followerList = JSON.parse(info);
                        resolve(followerList.map((relation: any) => relation.followerID));
                    },
                    (error: any) => {
                        console.error("获取粉丝列表失败", error);
                        reject(error);
                    }
                )
            });

            const newFollowers = await Promise.all(
                newFollowersID.map(async (id) => {
                    // 获取用户信息
                    const userInfo = await new Promise<UserInfo>((resolve, reject) => {
                        new QueryUserInfoMessage(id).send(
                            (info: string) => {
                                resolve(JSON.parse(info) as UserInfo);
                            },
                            (error: any) => {
                                console.error("获取用户信息失败", error);
                                reject(error);
                            }
                        );
                    });

                    // 获取关注状态 - 只有当当前用户已登录时才查询
                    let following = false;
                    if (currentUserID) {
                        try {
                            following = await new Promise<boolean>((resolve, reject) => {
                                new QueryFollowMessage(currentUserID, id).send(
                                    (info: string) => {
                                        resolve(JSON.parse(info));
                                    },
                                    (error: any) => {
                                        console.error("获取关注状态失败", error);
                                        resolve(false); // 出错时默认未关注
                                    }
                                );
                            });
                        } catch (error) {
                            console.error("查询关注状态失败", error);
                            following = false;
                        }
                    }

                    return {
                        userInfo,
                        following
                    } as FollowerWithStatus;
                })
            );

            const hasMore = newFollowers.length === perpage;

            if (page === 1) {
                setFollowers(newFollowers);
            } else {
                setFollowers(prev => [...prev, ...newFollowers]);
            }
            setHasMore(hasMore);
        } catch (error) {
            console.error("获取粉丝列表失败", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchFollowers(page);
    }, [page, refreshTag]);

    const handleLoadMore = () => {
        setPage(prev => prev + 1);
    };

    // 处理关注/取消关注
    const handleFollowToggle = async (followerIndex: number) => {
        const follower = followers[followerIndex];
        const targetUserID = follower.userInfo.userID;

        // 检查是否已登录
        if (!currentUserID || !userToken) {
            console.warn("用户未登录，无法执行关注操作");
            return;
        }

        // 检查是否关注自己
        if (currentUserID === targetUserID) {
            console.warn("不能关注自己");
            return;
        }

        try {
            // 立即更新UI状态
            setFollowers(prev => prev.map((f, index) =>
                index === followerIndex
                    ? { ...f, following: !f.following }
                    : f
            ));

            // 发送关注状态更改请求
            await new Promise<void>((resolve, reject) => {
                new ChangeFollowStatusMessage(
                    userToken,
                    targetUserID,
                    !follower.following
                ).send(
                    (info: string) => {
                        console.log("关注状态更改成功", info);
                        resolve();
                    },
                    (error: any) => {
                        console.error("关注状态更改失败", error);
                        // 如果失败，恢复原状态
                        setFollowers(prev => prev.map((f, index) =>
                            index === followerIndex
                                ? { ...f, following: follower.following }
                                : f
                        ));
                        reject(error);
                    }
                );
            });

            refreshUserStat();
        } catch (error) {
            console.error("关注操作失败", error);
        }
    };

    // 处理用户点击跳转到主页
    const handleUserClick = (userID: number) => {
        navigateHome(userID);
    };

    return (
        <>
            <div className="home-tab-title">粉丝列表</div>
            <div className="home-followers-tab">
                <div className="home-user-list">
                    {followers.map((follower, index) => (
                        <div key={follower.userInfo.userID} className="home-user-item">
                            <div className="home-user-avatar" onClick={() => handleUserClick(follower.userInfo.userID)}>
                                <img src={follower.userInfo.avatarPath} alt="用户头像" />
                            </div>
                            <div className="home-user-info">
                                <div className="home-user-name" onClick={() => handleUserClick(follower.userInfo.userID)}>
                                    {follower.userInfo.username}
                                </div>
                                <div className="home-user-bio">{follower.userInfo.bio}</div>
                            </div>
                            {/* 只有当前用户已登录且不是自己时才显示关注按钮 */}
                            {currentUserID && currentUserID !== follower.userInfo.userID && (
                                <button
                                    className={`home-follow-btn ${follower.following ? 'following' : ''}`}
                                    onClick={() => handleFollowToggle(index)}
                                >
                                    {follower.following ? '已关注' : '关注'}
                                </button>
                            )}
                        </div>
                    ))}
                </div>

                {hasMore && (
                    <div className="home-load-more" onClick={handleLoadMore}>
                        {loading ? "加载中..." : "加载更多"}
                    </div>
                )}
            </div>
        </>
    );
};

export default FollowersTab;