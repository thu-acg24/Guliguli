/**
 * ReportComment
 * desc: 评论举报记录
 * @param reportID: Int (举报记录的唯一标识符)
 * @param commentID: Int (被举报的评论的唯一标识符)
 * @param reporterID: Int (举报人的唯一标识符)
 * @param reason: String (举报原因描述)
 * @param status: ReportStatus:1016 (举报记录的状态)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { ReportStatus } from 'Plugins/ReportService/Objects/ReportStatus';


export class ReportComment extends Serializable {
    constructor(
        public  reportID: number,
        public  commentID: number,
        public  reporterID: number,
        public  reason: string,
        public  status: ReportStatus
    ) {
        super()
    }
}


