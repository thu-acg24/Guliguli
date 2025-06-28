/**
 * UserStat
 * desc: 用户的统计信息，包括粉丝、关注、视频数和收藏夹视频数
 * @param followerCount: Int (粉丝数量)
 * @param followingCount: Int (关注的人数)
 * @param videoCount: Int (上传的视频数量)
 * @param favoriteVideoCount: Int (收藏夹中的视频数量)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class UserStat extends Serializable {
    constructor(
        public  followerCount: number,
        public  followingCount: number,
        public  videoCount: number,
        public  favoriteVideoCount: number
    ) {
        super()
    }
}


