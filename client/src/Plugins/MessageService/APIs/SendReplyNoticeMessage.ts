/**
 * SendMessageMessage
 * desc: 回复评论后，发送通知给被回复者。
 * @param token: String (用户的身份令牌，用于验证身份。)
 * @param commentID: Int (评论的ID。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class SendNotificationMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  commentID: number,
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Message"]
    }
}

