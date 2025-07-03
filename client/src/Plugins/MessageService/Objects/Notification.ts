/**
 * Notification
 * desc: 消息实体，包含发送方、接收方等基本信息
 * @param notificationID: Int (通知的唯一ID)
 * @param content: String (消息内容)
 * @param timestamp: DateTime (消息发送的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class Notification extends Serializable {
    constructor(
        public  notificationID: number,
        public  title: string,
        public  content: string,
        public  timestamp: string,
    ) {
        super()
    }
}


