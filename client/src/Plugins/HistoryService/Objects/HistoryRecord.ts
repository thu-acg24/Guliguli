/**
 * HistoryRecord
 * desc: 用户观看历史记录
 * @param historyID: Int (历史记录唯一ID)
 * @param userID: Int (用户的唯一ID)
 * @param videoID: Int (视频的唯一ID)
 * @param timestamp: DateTime (观看时间的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class HistoryRecord extends Serializable {
    constructor(
        public  historyID: number,
        public  userID: number,
        public  videoID: number,
        public  timestamp: number
    ) {
        super()
    }
}


