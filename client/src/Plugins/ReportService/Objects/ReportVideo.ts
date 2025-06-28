/**
 * ReportVideo
 * desc: 举报视频的数据结构，包含举报的基本信息
 * @param reportID: Int (举报记录的唯一标识)
 * @param videoID: Int (被举报视频的唯一标识)
 * @param reporterID: Int (举报用户的唯一标识)
 * @param reason: String (举报的原因)
 * @param status: ReportStatus:1016 (处理该举报的状态)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'

import { ReportStatus } from 'Plugins/ReportService/Objects/ReportStatus';


export class ReportVideo extends Serializable {
    constructor(
        public  reportID: number,
        public  videoID: number,
        public  reporterID: number,
        public  reason: string,
        public  status: ReportStatus
    ) {
        super()
    }
}


