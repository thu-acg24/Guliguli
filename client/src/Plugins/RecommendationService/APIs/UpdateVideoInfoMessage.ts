/**
 * UpdateVideoInfoMessage
 * desc: 修改视频元数据。
 * @param token: String (用户认证token，用于验证用户合法性。)
 * @param videoID: Int (视频ID)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';


export class UpdateVideoInfoMessage extends TongWenMessage {
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

