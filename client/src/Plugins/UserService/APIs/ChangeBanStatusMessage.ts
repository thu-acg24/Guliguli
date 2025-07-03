/**
 * ChangeBanStatusMessage
 * desc: 根据用户Token校验审核员权限后，封禁或解封指定用户并更新用户表状态。用于修改用户封禁状态功能点
 * @param token: String (用户身份验证的Token，用于校验身份及权限。)
 * @param userID: Int (需要修改封禁状态的目标用户ID。)
 * @param isBan: Boolean (标识是否封禁用户，true为封禁，false为解封。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class ChangeBanStatusMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  userID: number,
        public  isBan: boolean
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

