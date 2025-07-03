/**
 * QueryUserStatMessage
 * desc: 根据用户ID查询用户表，返回用户的统计数据。
 * @param userID: Int (用户唯一标识，表示需要查询的用户ID。)
 * @return user: UserStat (用户统计数据，追随者数量、粉丝数量等。)
 */
import { TongWenMessage } from 'Plugins/TongWenAPI/TongWenMessage'
import { ServerAddr } from '../../../server-config';



export class QueryUserStatMessage extends TongWenMessage {
    constructor(
        public  userID: number
    ) {
        super()
    }
    getAddress(): string {
        return ServerAddr["User"]
    }
}

