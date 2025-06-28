/**
 * QueryAuditorsMessage
 * desc: 根据用户Token校验管理员权限后，查询所有审核员的列表。
 * @param token: String (用户身份标识的令牌，用于校验当前登录用户身份。)
 * @return auditors: UserInfo:1100 (审核员的列表信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryAuditorsMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}

