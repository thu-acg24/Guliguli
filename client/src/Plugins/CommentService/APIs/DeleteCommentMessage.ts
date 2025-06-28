/**
 * DeleteCommentMessage
 * desc: 根据用户Token校验权限后，删除评论
 * @param token: String (用户认证令牌，用于校验用户身份)
 * @param commentID: Int (目标评论的ID)
 * @return result: String (操作结果，可选返回错误信息)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class DeleteCommentMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  commentID: number
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10010"
    }
}

