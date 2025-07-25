/**
 * UpdateFeedbackFavoriteMessage
 * desc: 根据用户的收藏或取消收藏行为，更新相应记录。
 * @param token: String (用户的鉴权Token，用于验证用户身份)
 * @param videoID: Int (需要操作的目标视频ID)
 * @param isFavorite: Boolean (标志是否收藏该视频，true为收藏，false为取消收藏)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class UpdateFeedbackFavoriteMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  isFavorite: boolean
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Recommendation"]
    }
}

