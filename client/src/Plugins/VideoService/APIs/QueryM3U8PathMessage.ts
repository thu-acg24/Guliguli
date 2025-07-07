
/**
 * QueryM3U8PathMessage
 * desc: 根据用户Token校验身份后，获取视频播放链接
 * @param token: String (用户身份校验的Token)
 * @param videoID: Int (视频ID)
 * @return M3U8Path: String (播放链接)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryM3U8PathMessage extends TongWenMessage {
    constructor(
        public  token: string | null,
        public  videoID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}
