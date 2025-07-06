/**
 * QueryLikedBatchMessage
 * desc: 根据commentID列表获取用户是否点赞过评论
 * @param token: String (用户认证令牌，用于校验用户身份)
 * @param commentIDs: Int[] (带查询评论列表)
 * @return liked: Boolean[] (是否点赞过对应评论)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryLikedBatchMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  commentIDs: number[]
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Comment"]
    }
}

