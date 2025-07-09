// src/Pages/HomePage/FollowingTab.tsx
import React, { useState, useEffect } from "react";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { useOutletContext } from "react-router-dom";
import { useNavigateHome } from "Globals/Navigate";
import { QueryFollowingListMessage } from "Plugins/UserService/APIs/QueryFollowingListMessage";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import { QueryFollowMessage } from "Plugins/UserService/APIs/QueryFollowMessage";
import { ChangeFollowStatusMessage } from "Plugins/UserService/APIs/ChangeFollowStatusMessage";
import { useUserIDValue, useUserToken } from "Globals/GlobalStore";
import "./HomePage.css";

const perpage = 10; // 每次新显示的关注数量

// 定义包含关注状态的用户信息接口
interface FollowingWithStatus {
    userInfo: UserInfo;
    following: boolean;
}

const FollowingTab: React.FC = () => {
    const outlet = useOutletContext<{ userID: number, refreshUserStat: () => void }>();
    const userID = outlet.userID;
    const refreshUserStat = outlet.refreshUserStat;

    const { navigateHome } = useNavigateHome();
    const currentUserID = useUserIDValue();
    const userToken = useUserToken();

    const [following, setFollowing] = useState<FollowingWithStatus[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(true);

    // 获取用户关注的用户
    const fetchFollowing = async (page: number) => {
        setLoading(true);
        try {
            const newFollowingID = await new Promise<number[]>((resolve, reject) => {
                new QueryFollowingListMessage(userID, (page - 1) * perpage + 1, page * perpage + 1).send(
                    (info: string) => {
                        const followingList = JSON.parse(info);
                        resolve(followingList.map((relation: any) => relation.followeeID));
                    },
                    (error: any) => {
                        console.error("获取关注列表失败", error);
                        reject(error);
                    }
                )
            });

            const newFollowing = await Promise.all(
                newFollowingID.map(async (id) => {
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
                    } as FollowingWithStatus;
                })
            );

            const hasMore = newFollowing.length === perpage;

            setFollowing(prev => [...prev, ...newFollowing]);
            setHasMore(hasMore);
        } catch (error) {
            console.error("获取关注列表失败", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchFollowing(page);
    }, [page]);

    const handleLoadMore = () => {
        setPage(prev => prev + 1);
    };

    // 处理关注/取消关注
    const handleFollowToggle = async (followingIndex: number) => {
        const followingUser = following[followingIndex];
        const targetUserID = followingUser.userInfo.userID;

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
            setFollowing(prev => prev.map((f, index) =>
                index === followingIndex
                    ? { ...f, following: !f.following }
                    : f
            ));

            // 发送关注状态更改请求
            await new Promise<void>((resolve, reject) => {
                new ChangeFollowStatusMessage(
                    userToken,
                    targetUserID,
                    !followingUser.following
                ).send(
                    (info: string) => {
                        console.log("关注状态更改成功", info);
                        resolve();
                    },
                    (error: any) => {
                        console.error("关注状态更改失败", error);
                        // 如果失败，恢复原状态
                        setFollowing(prev => prev.map((f, index) =>
                            index === followingIndex
                                ? { ...f, following: followingUser.following }
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
            <div className="home-tab-title">关注列表</div>
            <div className="home-following-tab">
                <div className="home-user-list">
                    {following.map((followingUser, index) => (
                        <div key={followingUser.userInfo.userID} className="home-user-item">
                            <div className="home-user-avatar" onClick={() => handleUserClick(followingUser.userInfo.userID)}>
                                <img src={followingUser.userInfo.avatarPath} alt="用户头像" />
                            </div>
                            <div className="home-user-info">
                                <div className="home-user-name" onClick={() => handleUserClick(followingUser.userInfo.userID)}>
                                    {followingUser.userInfo.username}
                                </div>
                                <div className="home-user-bio">{followingUser.userInfo.bio}</div>
                            </div>
                            {/* 只有当前用户已登录且不是自己时才显示关注按钮 */}
                            {currentUserID && currentUserID !== followingUser.userInfo.userID && (
                                <button
                                    className={`home-follow-btn ${followingUser.following ? 'following' : ''}`}
                                    onClick={() => handleFollowToggle(index)}
                                >
                                    {followingUser.following ? '已关注' : '关注'}
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

export default FollowingTab;