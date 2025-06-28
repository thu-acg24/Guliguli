/**
 * QueryPendingVideosMessage
 * desc: 获取所有待审核的视频信息
 * @param token: String (用于验证身份的令牌)
 * @return pendingVideos: Video:1120 (封装所有待审核视频的列表)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryPendingVideosMessage extends TongWenMessage {
    constructor(
        public  token: string
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10016"
    }
}

