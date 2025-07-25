/**
 * ModifyUserInfoMessage
 * desc: 根据用户Token校验身份后，更新用户表中newField对应字段的值，用于用户信息修改功能点
 * @param token: String (用户的身份验证Token)
 * @param newField: UserInfo:1100 (需要更新的用户字段值)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';
import { UserInfo } from 'Plugins/UserService/Objects/UserInfo';


export class ModifyUserInfoMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  newField: UserInfo
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

