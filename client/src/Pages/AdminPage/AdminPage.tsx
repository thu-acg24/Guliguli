import React, { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router";
import Header from "Components/Header/Header";
import { useUserRole } from "Hooks/useUserRole";
import { useNavigateMain } from "Globals/Navigate";
import { ChangeUserRoleMessage } from "Plugins/UserService/APIs/ChangeUserRoleMessage";
import { QueryAuditorsMessage } from "Plugins/UserService/APIs/QueryAuditorsMessage";
import { UserRole } from "Plugins/UserService/Objects/UserRole";
import { useUserToken } from "Globals/GlobalStore";
import { UserInfo } from "Plugins/UserService/Objects/UserInfo";
import { useTopSuccessToast } from "Components/TopSuccessToast/useTopSuccessToast";
import { useNavigateHome } from "Globals/Navigate";
import "./AdminPage.css";

export const adminPagePath = "/admin";
export function useNavigateAdmin() {
    const navigate = useNavigate();
    const navigateAdmin = useCallback(() => {
        navigate(adminPagePath);
    }, [navigate]);
    return { navigateAdmin };
}

const AdminPage: React.FC = () => {
    const { navigateMain } = useNavigateMain();
    const { loading, error, isAdmin } = useUserRole();
    const [userIdInput, setUserIdInput] = useState("");
    const [message, setMessage] = useState("");
    const [isSuccess, setIsSuccess] = useState(false);
    const [auditors, setAuditors] = useState<UserInfo[]>([]);
    const [listLoading, setListLoading] = useState(true);
    const token = useUserToken();
    const { ToastComponent, showSuccess } = useTopSuccessToast();
    const { navigateHome } = useNavigateHome();

    // 权限校验
    useEffect(() => {
        if (loading) return;
        if (error || !isAdmin) {
            navigateMain();
        }
    }, [loading, error, isAdmin]);

    // 获取审核员列表
    const fetchAuditors = useCallback(() => {
        setListLoading(true);
        new QueryAuditorsMessage(token).send(
            (info: string) => {
                setAuditors(JSON.parse(info));
                setListLoading(false);
            },
            (err: string) => {
                console.error("获取审核员列表失败:", err);
                setAuditors([]);
                setListLoading(false);
            }
        );
    }, []);

    useEffect(() => {
        if (!loading && isAdmin) {
            fetchAuditors();
        }
    }, [loading, isAdmin, fetchAuditors]);

    // 修改用户角色
    const handleChangeRole = async (userID: number, newRole: UserRole) => {
        return new Promise<void>((resolve, reject) => {
            new ChangeUserRoleMessage(token, userID, newRole).send(
                (info: string) => {
                    fetchAuditors();
                    resolve();
                },
                (err: string) => reject(new Error(err))
            )
        });
    };

    // 修改栏按钮
    const handleSetAuditor = async () => {
        const id = parseInt(userIdInput);
        if (!id) {
            setIsSuccess(false);
            setMessage("请输入有效的用户ID");
            return;
        }
        try {
            setMessage("");
            await handleChangeRole(id, UserRole.auditor);
            setIsSuccess(true);
            setMessage("操作成功！");
        } catch (error) {
            setIsSuccess(false);
            setMessage("操作失败，请稍后重试！");
            console.error("设置审核员失败:", error);
        }
    };
    const handleSetNormal = async () => {
        const id = parseInt(userIdInput);
        if (!id) {
            setIsSuccess(false);
            setMessage("请输入有效的用户ID");
            return;
        }
        try {
            setMessage("");
            await handleChangeRole(id, UserRole.normal);
            setIsSuccess(true);
            setMessage("操作成功！");
        } catch (error) {
            setIsSuccess(false);
            setMessage("操作失败，请稍后重试！");
            console.error("设置审核员失败:", error);
        }
    };
    const handleDeleteAuditor = async (userID: number) => {
        try {
            await handleChangeRole(userID, UserRole.normal);
            showSuccess("降级成功！");
        } catch (error) {
            console.error("降级审核员失败:", error);
        }
    };

    if (loading) {
        return (
            <div className="admin-page">
                <Header />
                <div className="admin-loading">加载中...</div>
            </div>
        );
    }
    if (!isAdmin) return null;

    const handleUserClick = (userID: number) => {
        navigateHome(userID);
    }

    return (
        <div className="admin-page">
            <Header />
            {ToastComponent}
            <div className="admin-container">
                {/* 修改栏单独卡片 */}
                <div className="admin-section-card">
                    <div className="admin-list-title">修改审核权限</div>
                    <div className="admin-modify-bar">
                        <div className="admin-modify-inner">
                            <input
                                className="admin-userid-input"
                                type="number"
                                placeholder="请输入用户ID"
                                value={userIdInput}
                                onChange={e => setUserIdInput(e.target.value)}
                            />
                            <button className="admin-btn set-auditor" onClick={handleSetAuditor} >增加审核权限</button>
                            <button className="admin-btn set-normal" onClick={handleSetNormal} >删除审核权限</button>
                        </div>
                    </div>
                    {message && (
                        <div className="admin-modify-bar">
                            <div className={isSuccess ? "admin-success-message" : "admin-error-message"}>
                                {message}
                            </div>
                        </div>
                    )}
                </div>
                {/* 列表栏单独卡片 */}
                <div className="admin-section-card">
                    <div className="admin-list-title">当前所有审核员</div>
                    {listLoading ? (
                        <div className="admin-list-loading">加载中...</div>
                    ) : (
                        <div className="admin-users-grid">
                            {auditors.length === 0 ? (
                                <div style={{ textAlign: "center", color: "#aaa", padding: "32px 0", gridColumn: "1 / -1" }}>暂无审核员</div>
                            ) : (
                                auditors.map(user => (
                                    <div className="admin-user-item" key={user.userID}>
                                        <div className="admin-user-avatar" onClick={() => handleUserClick(user.userID)}>
                                            <img src={user.avatarPath} alt="用户头像" />
                                        </div>
                                        <div className="admin-user-info">
                                            <div className="admin-user-name" onClick={() => handleUserClick(user.userID)}>{user.username}</div>
                                            <div className="admin-user-bio">{user.bio}</div>
                                        </div>
                                        <button
                                            className="admin-btn downgrade-btn"
                                            onClick={() => handleDeleteAuditor(user.userID)}
                                        >降级</button>
                                    </div>
                                ))
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div >
    );
};

export default AdminPage;
