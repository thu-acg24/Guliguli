/**
 * LikeRecord
 * desc: 用户点赞视频的记录
 * @param userID: Int (用户的唯一ID)
 * @param videoID: Int (视频的唯一ID)
 * @param timestamp: DateTime (点赞的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class LikeRecord extends Serializable {
    constructor(
        public  userID: number,
        public  videoID: number,
        public  timestamp: number
    ) {
        super()
    }
}


