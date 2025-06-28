/**
 * ChangeUserRoleMessage
 * desc: 根据用户Token校验管理员权限后，指定用户成为审核员或将用户设为普通用户。
 * @param token: String (用户登录后获取的唯一身份标识，用于校验用户身份。)
 * @param userID: Int (目标用户的唯一标识符，用于指定需要更改角色的用户。)
 * @param newRole: UserRole:1084 (指定角色的新值，用于将用户设置为审核员或普通用户。)
 * @return result: String (操作结果，返回错误信息或成功状态。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { UserRole } from 'Plugins/UserService/Objects/UserRole';


export class ChangeUserRoleMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  userID: number,
        public  newRole: UserRole
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}

