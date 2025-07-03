/**
 * QueryLikeVideosMessage
 * desc: 查询用户喜欢的所有视频。
 * @param userID: number (用户唯一标识符)
 * @return videos: Video[] (用户喜欢的所有视频列表)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryLikeVideosMessage extends TongWenMessage {
    constructor(
        public  userID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}

