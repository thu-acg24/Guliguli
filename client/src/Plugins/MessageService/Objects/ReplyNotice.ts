/**
 * CommentReply
 * desc: 回复通知
 * @param noticeID: Int (通知的唯一ID)
 * @param senderID: Int (回复发布者ID)
 * @param content: String (回复内容)
 * @param commentID: Int (回复ID)
 * @param originalContent: String (被回复评论内容)
 * @param originalCommentID: Int (评论ID)
 * @param timestamp: DateTime (消息发送的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class ReplyNotice extends Serializable {
    constructor(
        public  noticeID: number,
        public  senderID: number,
        public  content: string,
        public  commentID: number,
        public  originalContent: string,
        public  originalCommentID: number,
        public  timestamp: string
    ) {
        super()
    }
}


