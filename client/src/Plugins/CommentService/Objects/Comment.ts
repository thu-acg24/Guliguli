/**
 * Comment
 * desc: 评论详情
 * @param commentID: Int (评论的唯一标识符)
 * @param content: String (评论的内容)
 * @param videoID: Int (视频的唯一标识符，被评论的视频)
 * @param authorID: Int (评论作者的唯一标识符)
 * @param replyToID: Int (被回复的评论ID, 如果为空则表示不是回复)
 * @param replyToUserID Int (被回复的用户ID,如果为空则表示不是回复)
 * @param likes: Int (点赞数)
 * @param replyCount: Int (回复数)
 * @param timestamp: DateTime (评论创建的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class Comment extends Serializable {
    constructor(
        public  commentID: number,
        public  content: string,
        public  videoID: number,
        public  authorID: number,
        public  replyToID: number | null,
        public  replyToUserID: number|null,
        public  likes: number,
        public  replyCount: number,
        public  timestamp: string
    ) {
        super()
    }
}


