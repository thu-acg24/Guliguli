/**
 * ModifyPasswordMessage
 * desc: 根据用户Token和旧密码校验身份后，更新用户表中密码哈希的值，用于用户信息修改功能点。
 * @param token: String (用户的身份验证令牌)
 * @param oldPassword: String (用户的原始密码，用于验证身份)
 * @param newPassword: String (用户的新密码，用于更新密码哈希)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class ModifyPasswordMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  oldPassword: string,
        public  newPassword: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

