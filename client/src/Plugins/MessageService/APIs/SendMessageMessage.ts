/**
 * SendMessageMessage
 * desc: 根据用户 Token 验证身份后，发送私信给目标用户。
 * @param token: String (用户的身份令牌，用于验证身份。)
 * @param receiverID: Int (接收方的用户ID。)
 * @param messageContent: String (发送的私信内容。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class SendMessageMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  receiverID: number,
        public  messageContent: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Message"]
    }
}

