/**
 * PublishCommentMessage
 * desc: 用于发布新的评论或回复评论。
 * @param token: String (用户的身份令牌，用于验证用户身份。)
 * @param videoID: Int (需要评论的视频的ID。)
 * @param commentContent: String (评论的具体内容。)
 * @param replyToCommentID: Int (回复的目标评论ID，可空。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class PublishCommentMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  commentContent: string,
        public  replyToCommentID: number | null
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Comment"]
    }
}

