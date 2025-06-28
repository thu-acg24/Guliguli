/**
 * WatchDetail
 * desc: 记录用户观看视频的详细信息
 * @param watchID: Int (观看记录的唯一标识)
 * @param userID: Int (与观看行为相关的用户的ID)
 * @param videoID: Int (观看的视频的唯一标识)
 * @param watchDuration: Float (用户观看视频的时长（秒数）)
 * @param timestamp: DateTime (观看行为发生的时间)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class WatchDetail extends Serializable {
    constructor(
        public  watchID: number,
        public  userID: number,
        public  videoID: number,
        public  watchDuration: number,
        public  timestamp: number
    ) {
        super()
    }
}


