/**
 * DeleteVideoInfoMessage
 * desc: 删除视频元数据
 * @param token: String (用户验证Token，用于验证当前用户的身份权限)
 * @param videoID: Int (视频的唯一标识符，表示需要删除的视频)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class DeleteVideoInfoMessage extends TongWenMessage {
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

