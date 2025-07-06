// src/Pages/HomePage/FollowersTab.tsx
import React, { useState, useEffect } from "react";
import { useOutletContext } from "react-router-dom";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { QueryFollowerListMessage } from "Plugins/UserService/APIs/QueryFollowerListMessage";
import { QueryUserInfoMessage } from "Plugins/UserService/APIs/QueryUserInfoMessage";
import "./HomePage.css";

const perpage = 10; // 每次新显示的粉丝数量

const FollowersTab: React.FC<{ userID?: number }> = (props) => {
    const outlet = useOutletContext<{ userID: number }>();
    const userID = props.userID ?? outlet?.userID;

    const [followers, setFollowers] = useState<UserInfo[]>([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [hasMore, setHasMore] = useState(true);

    // 获取用户的粉丝
    const fetchFollowers = async (page: number) => {
        setLoading(true);
        try {
            const newFollowersID = await new Promise<number[]>((resolve, reject) => {
                new QueryFollowerListMessage(userID, (page - 1) * perpage + 1, page * perpage).send(
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

            const newFollowers = await Promise.all(
                newFollowersID.map(async (id) => {
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

            const hasMore = newFollowers.length === perpage;

            // 模拟数据
            // const newFollowers = Array.from({ length: perpage }, (_, i) =>
            //     new UserInfo(
            //         i + (page - 1) * perpage,
            //         `粉丝${i + (page - 1) * perpage}`,
            //         "https://picsum.photos/50/50",
            //         false
            //     )
            // );
            // const hasMore = true

            setFollowers(prev => [...prev, ...newFollowers]);
            setHasMore(hasMore);
        } catch (error) {
            console.error("获取粉丝列表失败", error);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchFollowers(page);
    }, [page]);

    const handleLoadMore = () => {
        setPage(prev => prev + 1);
    };

    return (
        <div className="home-followers-tab">
            <div className="home-user-list">
                {followers.map(user => (
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

export default FollowersTab;