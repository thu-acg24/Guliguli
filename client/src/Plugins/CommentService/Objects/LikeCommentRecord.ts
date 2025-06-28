/**
 * LikeCommentRecord
 * desc: 用户点赞评论记录的数据结构
 * @param userID: Int (用户的唯一ID)
 * @param commentID: Int (评论的唯一ID)
 * @param timestamp: DateTime (记录点赞的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class LikeCommentRecord extends Serializable {
    constructor(
        public  userID: number,
        public  commentID: number,
        public  timestamp: number
    ) {
        super()
    }
}


