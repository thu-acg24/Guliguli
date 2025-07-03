/**
 * QueryFavoriteVideosMessage
 * desc: 查询用户收藏的所有视频。
 * @param userID: number (用户唯一标识符)
 * @return videos: Video[] (用户收藏的所有视频列表)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryFavoriteVideosMessage extends TongWenMessage {
    constructor(
        public  userID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}
