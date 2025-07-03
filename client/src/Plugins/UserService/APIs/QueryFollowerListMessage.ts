/**
 * QueryFollowerListMessage
 * desc: 查询某用户的粉丝列表，返回关注关系中的记录。返回按照关注时间第rangeL条到第rangeR条，均包含。
 * @param userID: Int (目标用户的唯一标识。)
 * @param rangeL: Int (要查询的粉丝列表的起始范围（包含）。)
 * @param rangeR: Int (要查询的粉丝列表的结束范围（包含）。)
 * @return followerList: FollowRelation[] (包含粉丝列表的关注关系列表，每个关系包括粉丝的相关信息。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryFollowerListMessage extends TongWenMessage {
    constructor(
        public  userID: number,
        public  rangeL: number,
        public  rangeR: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

