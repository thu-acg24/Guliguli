import React, { useEffect, useState } from "react";
import { QueryUsersMessage } from "Plugins/UserService/APIs/QueryUsersMessage";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { useNavigateHome } from "Globals/Navigate";

const SearchUsersTab: React.FC<{
    keyword: string;
}> = ({ keyword }) => {
    const [userResults, setUserResults] = useState<UserInfo[]>([]);
    const [loading, setLoading] = useState(true);
    const { navigateHome } = useNavigateHome();

    const fetchSearchResults = async (keyword: string) => {
        try {
            if (!keyword) {
                setUserResults([]);
                setLoading(false);
                return;
            }
            setLoading(true);
            const users = await new Promise<UserInfo[]>((resolve, reject) => {
                new QueryUsersMessage(keyword).send(
                    (info: string) => resolve(JSON.parse(info) as UserInfo[]),
                    (error: string) => reject(new Error(error))
                );
            });
            setUserResults(users);
        } catch (error) {
            console.error("搜索用户失败:", error);
            setUserResults([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchSearchResults(keyword);
    }, [keyword]);

    if (loading) {
        return <div style={{ textAlign: "center", color: "#888", padding: "32px 0" }}>加载中...</div>;
    }
    if (!loading && userResults.length === 0) {
        return <div style={{ textAlign: "center", color: "#aaa", padding: "32px 0" }}>暂无相关用户</div>;
    }
    return (
        <div className="search-users-grid">
            {userResults.map(user => (
                <div className="search-user-item" key={user.userID}>
                    <div className="search-user-avatar" onClick={() => navigateHome(user.userID)}>
                        <img src={user.avatarPath} alt="用户头像" />
                    </div>
                    <div className="search-user-info">
                        <div>
                            <span className="search-user-name" onClick={() => navigateHome(user.userID)}>{user.username}</span>
                        </div>
                        <div className="search-user-bio">{user.bio}</div>
                    </div>
                </div>
            ))}
        </div>
    );
};

export default SearchUsersTab;
