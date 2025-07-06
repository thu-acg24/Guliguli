// src/Pages/HomePage/FollowingTab.tsx
import React, { useState, useEffect } from "react";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { useOutletContext } from "react-router-dom";
import { QueryFollowingListMessage } from "Plugins/UserService/APIs/QueryFollowingListMessage";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import "./HomePage.css";

const perpage = 10; // 每次新显示的关注数量

const FollowingTab: React.FC<{ userID?: number }> = (props) => {
    const outlet = useOutletContext<{ userID: number }>();
    const userID = props.userID ?? outlet?.userID;

    const [following, setFollowing] = useState<UserInfo[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(true);

    // 获取用户关注的用户
    const fetchFollowing = async (page: number) => {
        setLoading(true);
        try {
            const newFollowingID = await new Promise<number[]>((resolve, reject) => {
                new QueryFollowingListMessage(userID, (page - 1) * perpage + 1, page * perpage).send(
                    (info: string) => {
                        const followerList = JSON.parse(info);
                        resolve(followerList.map((follower: any) => follower.followerID));
                    },
                    (error: any) => {
                        console.error("获取粉丝列表失败", error);
                        reject(error);
                    }
                )
            });

            const newFollowing = await Promise.all(
                newFollowingID.map(async (id) => {
                    return await new Promise<UserInfo>((resolve, reject) => {
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
                })
            );

            const hasMore = newFollowing.length === perpage;

            // 模拟数据
            // const newFollowings = Array.from({ length: perpage }, (_, i) =>
            //     new UserInfo(
            //         i + (page - 1) * perpage,
            //         `粉丝${i + (page - 1) * perpage}`,
            //         "https://picsum.photos/50/50",
            //         false
            //     )
            // );
            // const hasMore = true

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

    return (
        <div className="home-following-tab">
            <div className="home-user-list">
                {following.map(user => (
                    <div key={user.userID} className="home-user-item">
                        <div className="home-user-avatar">
                            <img src={user.avatarPath} alt="用户头像" />
                        </div>
                        <div className="home-user-info">
                            <div className="home-user-name">{user.username}</div>
                        </div>
                        <button className="home-follow-btn">关注</button>
                    </div>
                ))}
            </div>

            {hasMore && (
                <div className="home-load-more" onClick={handleLoadMore}>
                    {loading ? "加载中..." : "加载更多"}
                </div>
            )}
        </div>
    );
};

export default FollowingTab;