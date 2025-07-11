/**
 * QueryFollowingVideosMessage
 * desc: 根据用户ID获取其关注用户发布的视频列表
 * @param token: String (用户Token)
 * @param fetchLimit: Int (每次查询的最大视频数量)
 * @param lastTime: DateTime (上次查询的最后一个视频的发布时间，用于分页查询)
 * @param lastID: Int (上次查询的最后一个视频ID，用于分页查询)
 * @return video: List[Video] (用户发布的所有视频信息)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryFollowingVideosMessage extends TongWenMessage {
    constructor(
        public token: string,
        public fetchLimit: number,
        public lastTime: number,
        public lastID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}

