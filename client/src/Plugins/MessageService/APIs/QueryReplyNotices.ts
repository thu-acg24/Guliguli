/**
 * QueryUserInContactMessage
 * desc: 根据用户Token验证身份后，查询用户收到的所有回复通知。
 * @param token: String (用户的身份验证令牌。)
 * @return replyNotices: ReplyNotice[] (回复评论通知列表)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryReplyNoticesMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10013"
    }
}

