/**
 * Message
 * desc: 消息实体，包含发送方、接收方等基本信息
 * @param messageID: Int (消息的唯一ID)
 * @param senderID: Int (发送者的用户ID)
 * @param receiverID: Int (接收者的用户ID)
 * @param content: String (消息内容)
 * @param timestamp: DateTime (消息发送的时间戳)
 * @param isNotification: Boolean (是否是一条通知消息)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class Message extends Serializable {
    constructor(
        public  messageID: number,
        public  senderID: number,
        public  receiverID: number,
        public  content: string,
        public  timestamp: number,
        public  isNotification: boolean
    ) {
        super()
    }
}


