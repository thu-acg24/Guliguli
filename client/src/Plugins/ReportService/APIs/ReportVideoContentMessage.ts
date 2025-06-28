/**
 * ReportVideoContentMessage
 * desc: 根据用户Token检验身份后，将举报记录保存到视频举报表。
 * @param token: String (用户登录认证Token，用于识别用户身份。)
 * @param videoID: Int (被举报的视频ID。)
 * @param reason: String (举报理由。)
 * @return result: String (操作结果，可能为错误信息或空值。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class ReportVideoContentMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  reason: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10015"
    }
}

