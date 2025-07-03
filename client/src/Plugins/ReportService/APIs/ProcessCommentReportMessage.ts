/**
 * ProcessCommentReportMessage
 * desc: 根据用户Token验证审核员权限后，更新评论举报表中对应记录的状态。
 * @param token: String (用户身份验证的令牌)
 * @param reportID: Int (举报记录的唯一标识符)
 * @param status: ReportStatus (举报记录的新状态)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ReportStatus } from 'Plugins/ReportService/Objects/ReportStatus';


export class ProcessCommentReportMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  reportID: number,
        public  status: ReportStatus
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Report"]
    }
}

