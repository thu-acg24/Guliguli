/**
 * QueryFollowingListMessage
 * desc: 查询某用户的关注列表，返回他们关注的用户信息。
 * @param userID: Int (当前需要查询关注列表的目标用户ID。)
 * @param rangeL: Int (关注列表数据中提取的起始位置索引。)
 * @param rangeR: Int (关注列表数据中提取的结束位置索引。)
 * @return followList: FollowRelation[] (目标用户的关注记录列表。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'



export class QueryFollowingListMessage extends TongWenMessage {
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

