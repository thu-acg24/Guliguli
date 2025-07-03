/**
 * ModifyAvatarMessage
 * desc: 根据用户Token校验审核员权限后，封禁或解封指定用户并更新用户表状态。用于修改用户封禁状态功能点
 * @param token: String (用户身份验证的Token，用于校验身份及权限。)
 * @return result: String (上传URL)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';


export class ModifyAvatarMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

