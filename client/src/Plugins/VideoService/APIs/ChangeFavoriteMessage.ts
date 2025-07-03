/**
 * ChangeFavoriteMessage
 * desc: 增或删用户收藏记录。
 * @param token: String (用户身份认证Token)
 * @param videoID: Int (视频唯一标识符)
 * @param isFav: Boolean (表示是否收藏(true表示新增收藏，false表示取消收藏))
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class ChangeFavoriteMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  isFav: boolean
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}

