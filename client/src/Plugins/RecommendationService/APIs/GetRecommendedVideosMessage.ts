/**
 * GetRecommendedVideosMessage
 * desc: 基于用户行为或视频标签生成推荐视频列表
 * @param videoID: Int (视频ID，用于基于视频相关性生成推荐。)
 * @param userID: Int (用户ID，用于基于用户行为生成推荐。)
 * @param randomRatio: Float (随机性参数，用于增强推荐的随机性)
 * @return recommendedVideos: Video[] (推荐的视频列表，包含视频的完整信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class GetRecommendedVideosMessage extends TongWenMessage {
    constructor(
        public  videoID: number | null,
        public  userToken: string | null,
        public  randomRatio: number = 0.2
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Recommendation"]
    }
}

