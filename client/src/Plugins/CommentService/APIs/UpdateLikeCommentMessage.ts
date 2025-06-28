/**
 * UpdateLikeCommentMessage
 * desc: 用于点赞或取消点赞指定评论。
 * @param token: String (用户的身份认证令牌，用于校验用户身份。)
 * @param commentID: Int (目标评论的唯一标识符。)
 * @param isLike: Boolean (标记用户的操作类型，true表示点赞，false表示取消点赞。)
 * @return result: String (操作的结果信息，None表示成功，Some[String]表示失败的具体原因。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class UpdateLikeCommentMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  commentID: number,
        public  isLike: boolean
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}

