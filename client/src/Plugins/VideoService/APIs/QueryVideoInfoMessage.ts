/**
 * QueryVideoInfoMessage
 * desc: 根据视频ID获取视频详情。
 * @param token: String (用户Token（可选）)
 * @param videoId: Int (视频ID，用于唯一标识一个视频。)
 * @return video: Video (封装的视频详情对象，若视频不存在则返回None。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryVideoInfoMessage extends TongWenMessage {
    constructor(
        public  token: string | null,
        public  videoId: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Video"]
    }
}

