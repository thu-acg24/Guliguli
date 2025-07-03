/**
 * QueryUserVideosMessage
 * desc: 根据用户ID获取其发布的视频。
 * @param token: string | null (用户Token（可选）)
 * @param userId: number (用户ID)
 * @return videos: Video[] (用户发布的所有视频信息)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryUserVideosMessage extends TongWenMessage {
    constructor(
        public  token: string | null,
        public  userId: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}
