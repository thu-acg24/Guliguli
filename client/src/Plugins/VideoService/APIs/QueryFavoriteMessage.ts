/**
 * QueryFavoriteMessage
 * desc: 查询用户是否收藏某个视频。
 * @param token: string (用户身份认证Token)
 * @param videoID: number (视频唯一标识符)
 * @return isFavorited: boolean (true表示用户收藏了该视频，false表示未收藏)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryFavoriteMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10016"
    }
}
