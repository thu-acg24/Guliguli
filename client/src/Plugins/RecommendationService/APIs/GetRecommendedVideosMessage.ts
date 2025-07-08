/**
 * GetRecommendedVideosMessage
 * desc: 基于用户行为或视频标签生成推荐视频列表
 * @param videoID: Int | null (视频ID，用于基于视频相关性生成推荐。)
 * @param userToken: String | null (用户ID，用于基于用户行为生成推荐。)
 * @param randomRatio: Float (随机性参数，用于增强推荐的随机性)
 * @param fetchLimit: Int (获取的视频数量，默认20个)
 * @return recommendedVideos: Video[] (推荐的视频列表，包含视频的完整信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class GetRecommendedVideosMessage extends TongWenMessage {
    constructor(
        public  videoID: number | null,
        public  userToken: string | null,
        public  randomRatio: number = 0.2,
        public  fetchLimit: number = 20
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["Recommendation"]
    }
}

