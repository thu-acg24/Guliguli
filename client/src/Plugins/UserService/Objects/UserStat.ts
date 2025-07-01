/**
 * UserStat
 * desc: 用户的统计信息，包括粉丝、关注、视频数和收藏夹视频数
 * @param followerCount: Int (粉丝数量)
 * @param followingCount: Int (关注的人数)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class UserStat extends Serializable {
    constructor(
        public  followerCount: number,
        public  followingCount: number,
    ) {
        super()
    }
}


