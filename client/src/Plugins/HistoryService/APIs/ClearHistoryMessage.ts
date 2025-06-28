/**
 * ClearHistoryMessage
 * desc: 根据用户Token校验后，删除用户的所有历史记录。
 * @param token: String (用户的身份Token，用于身份验证。)
 * @return result: String (操作的结果状态，返回None表示成功或返回错误信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class ClearHistoryMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10017"
    }
}

