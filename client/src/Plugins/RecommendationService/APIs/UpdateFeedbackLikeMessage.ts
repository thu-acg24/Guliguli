/**
 * UpdateFeedbackLikeMessage
 * desc: 根据用户的点赞或取消点赞行为，更新相应记录。
 * @param token: String (用户身份验证令牌)
 * @param videoID: Int (唯一标识要操作的视频的ID)
 * @param isLike: Boolean (是否是点赞操作，true为点赞，false为取消点赞)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class UpdateFeedbackLikeMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  isLike: boolean
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Recommendation"]
    }
}

