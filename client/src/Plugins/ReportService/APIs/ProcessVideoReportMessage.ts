/**
 * ProcessVideoReportMessage
 * desc: 根据用户Token验证审核员权限后，更新视频举报表中对应记录的状态
 * @param token: String (用户认证的Token，用于校验用户身份和权限。)
 * @param reportID: Int (举报记录的唯一标识ID。)
 * @param status: ReportStatus (举报记录的新状态，例如待处理、已处理或驳回。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';
import { ReportStatus } from 'Plugins/ReportService/Objects/ReportStatus';


export class ProcessVideoReportMessage extends TongWenMessage {
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

