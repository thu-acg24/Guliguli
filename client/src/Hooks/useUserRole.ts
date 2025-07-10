import { useState, useEffect } from 'react';
import { useUserToken } from 'Globals/GlobalStore';
import { QueryUserRoleMessage } from 'Plugins/UserService/APIs/QueryUserRoleMessage';
import { UserRole } from 'Plugins/UserService/Objects/UserRole';

export const useUserRole = () => {
    const userToken = useUserToken();
    const [userRole, setUserRole] = useState<UserRole | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        setLoading(true);
        if (!userToken) {
            setUserRole(null);
            setLoading(false);
            setError(null);
            return;
        }

        const fetchUserRole = async () => {
            try {
                setError(null);

                const response = await new Promise<string>((resolve, reject) => {
                    new QueryUserRoleMessage(userToken).send(
                        (info: string) => resolve(info),
                        (error: string) => reject(new Error(error))
                    );
                });

                const role = JSON.parse(response) as UserRole;
                setUserRole(role);
            } catch (err) {
                const errorMessage = err instanceof Error ? err.message : '获取用户角色失败';
                setError(errorMessage);
                setUserRole(null);
            } finally {
                setLoading(false);
            }
        };

        fetchUserRole();
    }, [userToken]);

    return {
        userRole,
        loading,
        error,
        isAdmin: userRole === UserRole.admin,
        isAuditor: userRole === UserRole.auditor,
        isNormal: userRole === UserRole.normal
    };
};
