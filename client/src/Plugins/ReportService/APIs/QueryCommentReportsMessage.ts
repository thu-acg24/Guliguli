/**
 * QueryCommentReportsMessage
 * desc: 根据用户Token验证审核员权限后，查询评论举报记录。
 * @param token: String (用户身份验证令牌，用于校验用户。)
 * @return reports: ReportComment (查询结果，包含所有待处理的评论举报记录。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryCommentReportsMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Report"]
    }
}

