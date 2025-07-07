/**
 * RecordWatchDataMessage
 * desc: 根据用户观看视频的行为记录详细数据到观看数据表。
 * @param token: String (用户登录的身份标识Token)
 * @param videoID: Int (需要记录观看行为的视频ID)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class RecordWatchDataMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Recommendation"]
    }
}

