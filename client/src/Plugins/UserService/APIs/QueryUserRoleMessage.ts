/**
 * QueryUserRoleMessage
 * desc: 根据用户Token校验身份后，返回当前用户的基本信息。
 * @param token: String (用户的验证Token，用于校验用户身份。)
 * @return userRole: UserRole (用户的角色信息，如管理员、审核员或普通用户。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryUserRoleMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

