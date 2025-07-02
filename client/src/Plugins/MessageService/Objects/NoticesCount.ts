/**
 * Message
 * desc: 未读消息计数
 * @param messagesCount: Int (未读消息数)
 * @param notificationsCount: Int (未读通知数)
 * @param replyNoticesCount: Int (未读回复数)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class NoticesCount extends Serializable {
    constructor(
        public  messagesCount: number,
        public  notificationsCount: number,
        public  replyNoticesCount: number,
    ) {
        super()
    }
}


