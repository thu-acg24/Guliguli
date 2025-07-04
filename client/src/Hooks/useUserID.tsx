import { useState, useEffect } from 'react';
import { useUserToken, setUserToken } from 'Globals/GlobalStore';
import { GetUIDByTokenMessage } from 'Plugins/UserService/APIs/GetUIDByTokenMessage';
import { materialAlertError } from 'Plugins/CommonUtils/Gadgets/AlertGadget';

export const useUserID = () => {
    const [userID, setUserID] = useState<number | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const userToken = useUserToken();

    const fetchUserID = async () => {
        if (!userToken) {
            setUserID(null);
            setError(null);
            return;
        }

        setLoading(true);
        setError(null);

        try {
            const id = await new Promise<number>((resolve, reject) => {
                try {
                    new GetUIDByTokenMessage(userToken).send(
                        (info: string) => {
                            const userID = JSON.parse(info);
                            resolve(userID);
                        },
                        (e) => {
                            console.error('Token校验失败:', e);
                            materialAlertError(`Token校验失败`, "", () => {
                                setUserToken("");
                            });
                            reject(new Error('Token校验失败'));
                        }
                    );
                } catch (e) {
                    console.error('Token校验异常:', e);
                    materialAlertError(`Token校验失败`, "", () => {
                        setUserToken("");
                    });
                    reject(new Error('Token校验失败'));
                }
            });
            setUserID(id);
        } catch (err) {
            setError(err instanceof Error ? err.message : '获取用户ID失败');
            setUserID(null);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchUserID();
    }, [userToken]);

    return {
        userID,
        loading,
        error,
        refetch: fetchUserID
    };
};
