/**
 * QueryCommentByIDMessage
 * desc: 根据commentID获取对应评论的详细信息
 * @param commentID: Int (评论ID，用于唯一标识某个评论)
 * @return comment: Comment:1140 (返回的评论对象，包含评论的详细信息)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryCommentByIDMessage extends TongWenMessage {
    constructor(
        public  commentID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Comment"]
    }
}

