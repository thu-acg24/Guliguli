/**
 * RecordWatchDataMessage
 * desc: 根据用户观看视频的行为记录详细数据到观看数据表。
 * @param token: String (用户登录的身份标识Token)
 * @param videoID: Int (需要记录观看行为的视频ID)
 * @param watchDuration: Float (用户的观看时长，单位为秒)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class RecordWatchDataMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  watchDuration: number
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

