/**
 * LogoutMessage
 * desc: 根据用户Token，移除当前用户登录状态，并销毁存储的Token。用于用户登出功能点。
 * @param token: String (用户登录Token，用于校验用户身份)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class LogoutMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10012"
    }
}

