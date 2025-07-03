// src/Pages/HomePage/FollowingTab.tsx
import React, { useState, useEffect } from "react";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { useOutletContext } from "react-router-dom";
import "./HomePage.css";

interface FollowingTabProps {
    userID: number;
}

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
            // API调用留空
            // const result = await getUserFollowing(userID, page, 10);
            // setFollowing(prev => [...prev, ...result.following]);
            // setHasMore(result.hasMore);

            // 模拟数据
            const mockFollowing = Array.from({ length: 10 }, (_, i) =>
                new UserInfo(
                    i + (page - 1) * 10,
                    `用户${i + (page - 1) * 10}`,
                    "https://picsum.photos/50/50",
                    false
                )
            );

            setFollowing(prev => [...prev, ...mockFollowing]);
            setHasMore(true);
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