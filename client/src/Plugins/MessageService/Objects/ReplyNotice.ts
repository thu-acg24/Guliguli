/**
 * CommentReply
 * desc: 回复通知
 * @param noticeID: Int (通知的唯一ID)
 * @param senderID: Int (回复发布者ID)
 * @param receiverID: Int (被回复者ID)
 * @param content: String (回复评论内容)
 * @param commentID: Int (评论ID)
 * @param timestamp: DateTime (消息发送的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class Message extends Serializable {
    constructor(
        public  noticeID: number,
        public  senderID: number,
        public  receiverID: number,
        public  content: string,
        public  commentID: number,
        public  timestamp: number
    ) {
        super()
    }
}


