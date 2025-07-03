/**
 * QueryDanmakuReportsMessage
 * desc: 根据用户Token验证审核员权限后，查询弹幕举报记录。
 * @param token: String (用户身份验证令牌，用于校验用户的身份和权限。)
 * @return reports: ReportDanmaku (包含所有待处理的弹幕举报记录的列表，每条记录包括举报的详细信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryDanmakuReportsMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Report"]
    }
}

