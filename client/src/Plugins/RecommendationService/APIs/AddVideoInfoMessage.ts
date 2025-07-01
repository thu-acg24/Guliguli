/**
 * AddVideoInfoMessage
 * desc: 新增视频元数据
 * @param token: String (用户的身份验证令牌)
 * @param info: VideoInfo (视频的元数据信息，例如标题、描述、标签等)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { VideoInfo } from 'Plugins/RecommendationService/Objects/VideoInfo';


export class AddVideoInfoMessage extends TongWenMessage {
    constructor(
        public  token: string,
        public  info: VideoInfo
    ) {
        super()
    }
    getAddress(): string {
        return "127.0.0.1:10011"
    }
}

