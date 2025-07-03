/**
 * SendMessageMessage
 * desc: 根据用户 Token 验证身份后，发送通知给目标用户。
 * @param token: String (用户的身份令牌，用于验证身份。)
 * @param receiverID: Int (接收方的用户ID。)
 * @param messageContent: String (发送的通知的内容。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class SendNotificationMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  receiverID: number,
        public  messageContent: string,
        public  isNotification: boolean
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Message"]
    }
}

