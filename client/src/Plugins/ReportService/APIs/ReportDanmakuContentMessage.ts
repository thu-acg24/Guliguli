/**
 * ReportDanmakuContentMessage
 * desc: 根据用户Token验证身份后，将举报记录保存到弹幕举报表。
 * @param token: String (用户身份的认证令牌。)
 * @param danmakuID: Int (被举报的弹幕的唯一标识。)
 * @param reason: String (举报该弹幕的理由。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class ReportDanmakuContentMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  danmakuID: number,
        public  reason: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Report"]
    }
}

