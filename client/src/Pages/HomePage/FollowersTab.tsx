// src/Pages/HomePage/FollowersTab.tsx
import React, { useState, useEffect } from "react";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { useOutletContext } from "react-router-dom";
import "./HomePage.css";

interface FollowersTabProps {
    userID: number;
}

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
            // API调用留空
            // const result = await getUserFollowers(userID, page, 10);
            // setFollowers(prev => [...prev, ...result.followers]);
            // setHasMore(result.hasMore);

            // 模拟数据
            const mockFollowers = Array.from({ length: 10 }, (_, i) =>
                new UserInfo(
                    i + (page - 1) * 10,
                    `粉丝${i + (page - 1) * 10}`,
                    "https://picsum.photos/50/50",
                    false
                )
            );

            setFollowers(prev => [...prev, ...mockFollowers]);
            setHasMore(true);
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