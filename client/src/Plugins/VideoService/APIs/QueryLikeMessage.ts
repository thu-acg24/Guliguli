/**
 * QueryLikeMessage
 * desc: 查询用户是否喜欢某个视频。
 * @param token: string (用户身份认证Token)
 * @param videoID: number (视频唯一标识符)
 * @return isLiked: boolean (true表示用户喜欢该视频，false表示不喜欢)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryLikeMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}
