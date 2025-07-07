/**
 * QueryHistoryMessage
 * desc: 根据用户Token校验后，查询用户的观看历史记录，返回按照 (timestamp, ID) 开始降序排序20条记录
 * @param token: String (用户身份令牌，用于校验身份)
 * @param lastTime: DateTime (上一次查询到历史记录的浏览时间)
 * @param lastID: Int (上一次历史记录的ID)
 * @param fetchLimit: Int (至多获取多少条记录，不超过 100)
 * @return history: HistoryRecord[] (返回用户观看历史记录的列表)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryHistoryMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  lastTime: number,
        public  lastID: number,
        public  fetchLimit: number = 10
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["History"]
    }
}

