/**
 * QueryLikedBatchMessage
 * desc: 根据commentID列表获取用户是否点赞过评论
 * @param token: String (用户认证令牌，用于校验用户身份)
 * @param commentIds: Int[] (带查询评论列表)
 * @return liked: Boolean[] (是否点赞过对应评论)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryLikedBatchMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  commentIds: number[]
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}

