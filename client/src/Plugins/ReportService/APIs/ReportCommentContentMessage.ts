/**
 * ReportCommentContentMessage
 * desc: 根据用户Token验证身份后，将举报记录保存到评论举报表。
 * @param token: String (用户的身份校验令牌)
 * @param commentID: Int (被举报的评论ID)
 * @param reason: String (举报理由)
 * @return result: String (操作结果，若举报失败返回错误描述，若成功返回None)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class ReportCommentContentMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  commentID: number,
        public  reason: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10015"
    }
}

