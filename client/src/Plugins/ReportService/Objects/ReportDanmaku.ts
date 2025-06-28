/**
 * ReportDanmaku
 * desc: 弹幕举报信息
 * @param reportID: Int (举报记录的唯一ID)
 * @param danmakuID: Int (被举报的弹幕的唯一ID)
 * @param reporterID: Int (举报人的唯一ID)
 * @param reason: String (举报理由)
 * @param status: ReportStatus:1016 (举报处理状态)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { ReportStatus } from 'Plugins/ReportService/Objects/ReportStatus';


export class ReportDanmaku extends Serializable {
    constructor(
        public  reportID: number,
        public  danmakuID: number,
        public  reporterID: number,
        public  reason: string,
        public  status: ReportStatus
    ) {
        super()
    }
}


