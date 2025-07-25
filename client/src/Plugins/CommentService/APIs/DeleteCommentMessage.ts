/**
 * DeleteCommentMessage
 * desc: 根据用户Token校验权限后，删除评论
 * @param token: String (用户认证令牌，用于校验用户身份)
 * @param commentID: Int (目标评论的ID)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class DeleteCommentMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  commentID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Comment"]
    }
}

