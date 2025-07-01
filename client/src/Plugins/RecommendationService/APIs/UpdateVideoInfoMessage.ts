/**
 * UpdateVideoInfoMessage
 * desc: 修改视频元数据。
 * @param token: String (用户认证token，用于验证用户合法性。)
 * @param info: VideoInfo(包含视频数据信息的对象。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { VideoInfo } from 'Plugins/RecommendationService/Objects/VideoInfo';


export class UpdateVideoInfoMessage extends TongWenMessage {
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

