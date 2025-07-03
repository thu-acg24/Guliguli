/**
 * ClearHistoryMessage
 * desc: 根据用户Token校验后，删除用户的所有历史记录。
 * @param token: String (用户的身份Token，用于身份验证。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class ClearHistoryMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["History"]
    }
}

