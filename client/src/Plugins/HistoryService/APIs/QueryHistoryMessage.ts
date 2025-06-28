/**
 * QueryHistoryMessage
 * desc: 根据用户Token校验后，查询用户的观看历史记录，返回从新到旧第rangeL条到第rangeR条，均包含。
 * @param token: String (用户身份令牌，用于校验身份)
 * @param rangeL: Int (分页查询的起始位置)
 * @param rangeR: Int (分页查询的结束位置)
 * @return history: HistoryRecord:1184 (返回用户观看历史记录的列表)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryHistoryMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  rangeL: number,
        public  rangeR: number
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10017"
    }
}

