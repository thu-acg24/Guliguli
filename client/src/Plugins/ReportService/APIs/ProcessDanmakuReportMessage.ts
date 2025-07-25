/**
 * ProcessDanmakuReportMessage
 * desc: 根据用户Token验证审核员权限后，更新弹幕举报表中对应记录的状态。
 * @param token: String (用户认证Token，用于身份校验)
 * @param reportID: Int (举报记录的ID，用于标识具体举报记录)
 * @param status: ReportStatus (要更新的举报状态)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';
import { ReportStatus } from 'Plugins/ReportService/Objects/ReportStatus';


export class ProcessDanmakuReportMessage extends TongWenMessage {
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

