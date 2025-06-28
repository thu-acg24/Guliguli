/**
 * FavoriteRecord
 * desc: 视频收藏记录，记录用户收藏视频的相关信息
 * @param userID: Int (用户的唯一ID)
 * @param videoID: Int (视频的唯一ID)
 * @param timestamp: DateTime (收藏视频的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class FavoriteRecord extends Serializable {
    constructor(
        public  userID: number,
        public  videoID: number,
        public  timestamp: number
    ) {
        super()
    }
}


