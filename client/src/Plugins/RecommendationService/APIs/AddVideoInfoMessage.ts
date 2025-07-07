/**
 * AddVideoInfoMessage
 * desc: 新增视频元数据
 * @param token: String (用户的身份验证令牌)
 * @param videoID: Int (视频ID)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';


export class AddVideoInfoMessage extends TongWenMessage {
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

