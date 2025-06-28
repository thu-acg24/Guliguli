/**
 * FollowRelation
 * desc: 用户之间的关注关系
 * @param followerID: Int (关注者的用户ID)
 * @param followeeID: Int (被关注者的用户ID)
 * @param timestamp: DateTime (关注的时间戳)
 */
import { Serializable } from 'Plugins/CommonUtils/Send/Serializable'




export class FollowRelation extends Serializable {
    constructor(
        public  followerID: number,
        public  followeeID: number,
        public  timestamp: number
    ) {
        super()
    }
}


