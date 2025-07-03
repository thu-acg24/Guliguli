/**
 * QueryVideoReportsMessage
 * desc: 根据用户Token验证审核员权限后，查询视频举报记录。
 * @param token: String (用户认证的Token，用于权限校验)
 * @return reports: ReportVideo (视频举报记录的列表，每条记录包含举报详情字段)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryVideoReportsMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Report"]
    }
}

