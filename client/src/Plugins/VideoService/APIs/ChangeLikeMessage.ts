/**
 * ChangeLikeMessage
 * desc: 增或删用户点赞记录。
 * @param token: String (用户的校验Token，用于确认登录身份)
 * @param videoID: Int (视频的唯一标识符)
 * @param isLike: Boolean (点赞操作标志，true表示点赞，false表示取消点赞)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class ChangeLikeMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  videoID: number,
        public  isLike: boolean
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10016"
    }
}

