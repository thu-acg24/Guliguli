/**
 * ReportVideoContentMessage
 * desc: 根据用户Token检验身份后，将举报记录保存到视频举报表。
 * @param token: String (用户登录认证Token，用于识别用户身份。)
 * @param videoID: Int (被举报的视频ID。)
 * @param reason: String (举报理由。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class ReportVideoContentMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  reason: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Report"]
    }
}

