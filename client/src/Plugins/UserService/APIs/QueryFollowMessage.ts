/**
 * QueryFollowMessage
 * desc: 查询用户A是否关注用户B。
 * @param userA: Int (用户A的唯一标识符)
 * @param userB: Int (用户B的唯一标识符)
 * @return isFollowing: boolean (用户A是否关注用户B。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryFollowMessage extends TongWenMessage {
    constructor(
        public  userA: number,
        public  userB: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

